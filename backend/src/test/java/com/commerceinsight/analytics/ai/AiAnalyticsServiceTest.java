package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.config.AiProperties;
import com.commerceinsight.analytics.ai.dto.AiInsight;
import com.commerceinsight.analytics.ai.dto.AiInsightsResponse;
import com.commerceinsight.analytics.ai.llm.LlmClient;
import com.commerceinsight.analytics.ai.llm.LlmCompletion;
import com.commerceinsight.analytics.ai.llm.LlmException;
import com.commerceinsight.analytics.ai.llm.LlmFailureReason;
import com.commerceinsight.analytics.ai.llm.LlmRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiAnalyticsService} — request validation, graceful
 * degradation when the feature is off, provider-failure isolation (never
 * propagates; always {@code available:false}), and the observability counters.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiAnalyticsService")
class AiAnalyticsServiceTest {

    @Mock private LlmClient llmClient;
    @Mock private AiAnalyticsContextBuilder contextBuilder;
    @Mock private AiAnalyticsPromptBuilder promptBuilder;
    @Mock private AiAnalyticsResponseParser responseParser;

    private SimpleMeterRegistry registry;
    private AiAnalyticsService service;

    private final Instant from = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant to   = Instant.parse("2026-02-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new AiAnalyticsService(new AiProperties(), llmClient, contextBuilder, promptBuilder,
                responseParser, new AiMetrics(registry));
        lenient().when(promptBuilder.systemPrompt()).thenReturn("SYS");
        lenient().when(promptBuilder.userPrompt(any())).thenReturn("USER");
        lenient().when(llmClient.activeProviderId()).thenReturn("openai");
    }

    private double counter(String name) {
        var c = registry.find(name).counter();
        return c == null ? 0.0 : c.count();
    }

    @Test
    @DisplayName("dateFrom after dateTo → 400 AiAnalyticsException, no provider call, validation metric")
    void invalidRange_rejected() {
        assertThatThrownBy(() -> service.generate(to, from))
                .isInstanceOf(AiAnalyticsException.class);
        verify(llmClient, never()).complete(any());
        assertThat(counter("ai.insights.validation_failures")).isEqualTo(1.0);
        assertThat(counter("ai.insights.requests")).isZero();
    }

    @Test
    @DisplayName("dateFrom equal to dateTo → rejected")
    void equalRange_rejected() {
        assertThatThrownBy(() -> service.generate(from, from))
                .isInstanceOf(AiAnalyticsException.class);
    }

    @Test
    @DisplayName("window longer than max-range-days → rejected")
    void rangeTooLarge_rejected() {
        Instant farTo = from.plus(400, ChronoUnit.DAYS);
        assertThatThrownBy(() -> service.generate(from, farTo))
                .isInstanceOf(AiAnalyticsException.class);
        assertThat(counter("ai.insights.validation_failures")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("feature unavailable → safe response, no context build, no provider call, unavailable metric")
    void featureOff_degradesSafely() {
        when(llmClient.isAvailable()).thenReturn(false);

        AiInsightsResponse res = service.generate(from, to);

        assertThat(res.available()).isFalse();
        verify(contextBuilder, never()).build(any(), any());
        verify(llmClient, never()).complete(any());
        assertThat(counter("ai.insights.requests")).isEqualTo(1.0);
        assertThat(counter("ai.insights.unavailable")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("happy path → parsed structured response returned, success metric + latency timer")
    void success_returnsParsed() {
        when(llmClient.isAvailable()).thenReturn(true);
        when(contextBuilder.build(from, to)).thenReturn(dummyContext());
        when(llmClient.complete(any(LlmRequest.class)))
                .thenReturn(new LlmCompletion("{...}", "openai", "gpt-4o-mini"));
        AiInsightsResponse parsed = AiInsightsResponse.of("summary",
                List.of(new AiInsight("TREND", "t", "d", "", "LOW")), List.of(), "openai", "gpt-4o-mini");
        when(responseParser.parse("{...}", "openai", "gpt-4o-mini")).thenReturn(parsed);

        AiInsightsResponse res = service.generate(from, to);

        assertThat(res.available()).isTrue();
        assertThat(res.summary()).isEqualTo("summary");
        assertThat(res.provider()).isEqualTo("openai");
        assertThat(counter("ai.insights.success")).isEqualTo(1.0);
        assertThat(registry.find("ai.insights.latency").timer()).isNotNull();
    }

    @Test
    @DisplayName("provider throws LlmException(TIMEOUT) → unavailable + provider_failure{reason=TIMEOUT}")
    void providerFailure_isSwallowed_taggedByReason() {
        when(llmClient.isAvailable()).thenReturn(true);
        when(contextBuilder.build(from, to)).thenReturn(dummyContext());
        when(llmClient.complete(any(LlmRequest.class)))
                .thenThrow(new LlmException(LlmFailureReason.TIMEOUT, "AI provider timed out"));

        AiInsightsResponse res = service.generate(from, to);

        assertThat(res.available()).isFalse();
        assertThat(res.insights()).isEmpty();
        var c = registry.find("ai.insights.provider_failures").tag("reason", "TIMEOUT").counter();
        assertThat(c).isNotNull();
        assertThat(c.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("valid completion but parser degrades → provider_failure{reason=INVALID_RESPONSE}")
    void parserDegrades_countedAsInvalidResponse() {
        when(llmClient.isAvailable()).thenReturn(true);
        when(contextBuilder.build(from, to)).thenReturn(dummyContext());
        when(llmClient.complete(any(LlmRequest.class)))
                .thenReturn(new LlmCompletion("not json", "anthropic", "claude"));
        when(responseParser.parse("not json", "anthropic", "claude"))
                .thenReturn(AiInsightsResponse.unavailable());

        AiInsightsResponse res = service.generate(from, to);

        assertThat(res.available()).isFalse();
        assertThat(registry.find("ai.insights.provider_failures")
                .tag("reason", "INVALID_RESPONSE").counter()).isNotNull();
    }

    @Test
    @DisplayName("analytics context build failure → safe unavailable response")
    void contextFailure_isSwallowed() {
        when(llmClient.isAvailable()).thenReturn(true);
        when(contextBuilder.build(from, to)).thenThrow(new RuntimeException("analytics down"));

        AiInsightsResponse res = service.generate(from, to);

        assertThat(res.available()).isFalse();
        verify(llmClient, never()).complete(any());
        assertThat(counter("ai.insights.unavailable")).isEqualTo(1.0);
    }

    private static AiAnalyticsContext dummyContext() {
        return new AiAnalyticsContext(
                new AiAnalyticsContext.Window("a", "b", 31), "VND",
                new AiAnalyticsContext.Overview(java.math.BigDecimal.ZERO, 0, 0, 0, 0, 0,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO),
                List.of(), new AiAnalyticsContext.Growth(null, null, "n"),
                java.util.Map.of(), List.of(), java.util.Map.of(),
                new AiAnalyticsContext.Customers(0, 0, 0, java.math.BigDecimal.ZERO),
                new AiAnalyticsContext.Inventory(0, 0));
    }
}
