package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.config.AiProperties;
import com.commerceinsight.analytics.ai.dto.AiInsightsResponse;
import com.commerceinsight.analytics.ai.llm.LlmClient;
import com.commerceinsight.analytics.ai.llm.LlmCompletion;
import com.commerceinsight.analytics.ai.llm.LlmException;
import com.commerceinsight.analytics.ai.llm.LlmFailureReason;
import com.commerceinsight.analytics.ai.llm.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * AiAnalyticsService — orchestrates one AI-insights generation:
 *
 * <ol>
 *   <li>validate the date range (both bounds present via bean validation on the
 *       DTO; here: {@code from < to} and span ≤ {@code app.ai.max-range-days});</li>
 *   <li>if the feature is unavailable → return the safe degraded response;</li>
 *   <li>compose the compact context from the EXISTING analytics layer;</li>
 *   <li>build a deterministic system prompt + data context;</li>
 *   <li>call the configured provider through {@link LlmClient} (no retry);</li>
 *   <li>validate / bound the model output;</li>
 *   <li>on ANY provider-side failure → return the safe degraded response
 *       (HTTP stays 200; the dashboard keeps working).</li>
 * </ol>
 *
 * <p>Authorization is enforced upstream by {@code @PreAuthorize} on
 * {@link AiAnalyticsController} — identical to the rest of {@code /analytics/**}.
 * No LLM call is ever made from a repository, a controller, the frontend, or MCP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalyticsService {

    private final AiProperties props;
    private final LlmClient llmClient;
    private final AiAnalyticsContextBuilder contextBuilder;
    private final AiAnalyticsPromptBuilder promptBuilder;
    private final AiAnalyticsResponseParser responseParser;
    private final AiMetrics metrics;

    public AiInsightsResponse generate(Instant dateFrom, Instant dateTo) {
        validateRange(dateFrom, dateTo);
        metrics.request();

        long rangeDays = Duration.between(dateFrom, dateTo).toDays();
        String model = props.getModel();
        String provider = llmClient.activeProviderId();
        log.info("AI insights request started provider={} model={} rangeDays={}", provider, model, rangeDays);

        if (!llmClient.isAvailable()) {
            log.info("AI insights request completed provider={} model={} result=unavailable reason=not_configured",
                    provider, model);
            metrics.unavailable(provider, model);
            return AiInsightsResponse.unavailable();
        }

        AiAnalyticsContext context;
        try {
            context = contextBuilder.build(dateFrom, dateTo);
        } catch (RuntimeException e) {
            // Analytics itself failing is unexpected here — do not leak detail.
            log.warn("AI insights context build failed provider={} model={} reason={}", provider, model,
                    e.getClass().getSimpleName());
            metrics.unavailable(provider, model);
            return AiInsightsResponse.unavailable();
        }

        LlmRequest request = new LlmRequest(
                promptBuilder.systemPrompt(),
                promptBuilder.userPrompt(context),
                props.getTemperature(),
                props.getMaxOutputTokens(),
                true
        );

        long startedAt = System.nanoTime();
        try {
            LlmCompletion completion = llmClient.complete(request);
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            AiInsightsResponse parsed = responseParser.parse(
                    completion.content(), completion.provider(), completion.model());
            if (parsed.available()) {
                log.info("AI insights request completed provider={} model={} result=success latencyMs={}",
                        completion.provider(), completion.model(), latency.toMillis());
                metrics.success(completion.provider(), completion.model(), latency);
            } else {
                log.warn("AI insights request completed provider={} model={} result=invalid_response latencyMs={}",
                        completion.provider(), completion.model(), latency.toMillis());
                metrics.providerFailure(completion.provider(), completion.model(),
                        LlmFailureReason.INVALID_RESPONSE, latency);
            }
            return parsed;
        } catch (LlmException e) {
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            log.warn("AI insights provider failure provider={} model={} reason={} latencyMs={}",
                    provider, model, e.getReason(), latency.toMillis());
            metrics.providerFailure(provider, model, e.getReason(), latency);
            return AiInsightsResponse.unavailable();
        } catch (RuntimeException e) {
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            log.warn("AI insights unexpected failure provider={} model={} reason={} latencyMs={}",
                    provider, model, e.getClass().getSimpleName(), latency.toMillis());
            metrics.providerFailure(provider, model, LlmFailureReason.PROVIDER_ERROR, latency);
            return AiInsightsResponse.unavailable();
        }
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            metrics.validationFailure();
            throw AiAnalyticsException.invalidRange();
        }
        long days = Duration.between(from, to).toDays();
        if (days > props.getMaxRangeDays()) {
            metrics.validationFailure();
            throw AiAnalyticsException.rangeTooLarge(props.getMaxRangeDays());
        }
    }
}
