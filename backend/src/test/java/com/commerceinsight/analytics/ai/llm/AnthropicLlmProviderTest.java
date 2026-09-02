package com.commerceinsight.analytics.ai.llm;

import com.commerceinsight.analytics.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real-HTTP-path tests for {@link AnthropicLlmProvider} against a local
 * {@link MockLlmHttpServer}. Verifies the native Messages wire format
 * (x-api-key + anthropic-version headers, top-level system, assistant prefill)
 * and failure → {@link LlmFailureReason} mapping. No external API.
 */
@DisplayName("AnthropicLlmProvider")
class AnthropicLlmProviderTest {

    // Claude reply after a "{" prefill: it returns the REST of the object.
    private static final String VALID =
            "{\"content\":[{\"type\":\"text\",\"text\":\"\\\"summary\\\":\\\"ok\\\"}\"}],\"model\":\"claude\"}";

    private MockLlmHttpServer mock;
    private AiProperties props;

    @BeforeEach
    void setUp() throws Exception {
        mock = MockLlmHttpServer.start();
        props = new AiProperties();
        props.setEnabled(true);
        props.setProvider("anthropic");
        props.setModel("claude-3-5-haiku-latest");
        props.setApiKey("sk-ant-test");
        props.setAnthropicBaseUrl(mock.baseUrl());
        props.setTimeoutMs(2_000);
        props.setConnectTimeoutMs(1_000);
    }

    @AfterEach
    void tearDown() {
        mock.close();
    }

    private AnthropicLlmProvider provider() {
        return new AnthropicLlmProvider(props, new ObjectMapper());
    }

    private LlmRequest req() {
        return new LlmRequest("SYSTEM RULES", "DATA CONTEXT + TASK", 0.2, 900, true);
    }

    @Test
    @DisplayName("valid → completion; native endpoint, headers, system field, assistant prefill")
    void valid() {
        mock.respondWith(200, VALID);

        LlmCompletion c = provider().complete(req());

        assertThat(c.provider()).isEqualTo("anthropic");
        assertThat(c.content()).startsWith("{").contains("\"summary\":\"ok\"");
        assertThat(mock.lastPath.get()).isEqualTo("/v1/messages");
        assertThat(mock.lastHeaders.get("x-api-key")).isEqualTo("sk-ant-test");
        assertThat(mock.lastHeaders.get("anthropic-version")).isEqualTo("2023-06-01");
        assertThat(mock.lastHeaders).doesNotContainKey("authorization");
        assertThat(mock.lastBody.get())
                .contains("\"system\":\"SYSTEM RULES\"")
                .contains("\"role\":\"user\"")
                .contains("\"role\":\"assistant\"")
                .contains("\"content\":\"{\"");
    }

    @Test
    @DisplayName("id() and supports()")
    void identity() {
        AnthropicLlmProvider p = provider();
        assertThat(p.id()).isEqualTo("anthropic");
        assertThat(p.supports("anthropic")).isTrue();
        assertThat(p.supports("openai")).isFalse();
    }

    @Test
    @DisplayName("not configured without a key")
    void notConfiguredWithoutKey() {
        props.setApiKey("");
        assertThat(provider().isConfigured()).isFalse();
    }

    @Test
    @DisplayName("401 → UNAUTHORIZED, no key leak in message")
    void unauthorized() {
        mock.respondWith(401, "{\"error\":{\"message\":\"invalid x-api-key sk-ant-secret\"}}");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.UNAUTHORIZED)
                .extracting(Throwable::getMessage).asString().doesNotContain("sk-ant-secret");
    }

    @Test
    @DisplayName("429 → RATE_LIMITED")
    void rateLimited() {
        mock.respondWith(429, "{}");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.RATE_LIMITED);
    }

    @Test
    @DisplayName("500 → PROVIDER_ERROR")
    void serverError() {
        mock.respondWith(500, "overloaded");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.PROVIDER_ERROR);
    }

    @Test
    @DisplayName("malformed JSON → INVALID_RESPONSE")
    void malformedJson() {
        mock.respondWith(200, "not json at all");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("empty content array → INVALID_RESPONSE")
    void emptyContent() {
        mock.respondWith(200, "{\"content\":[]}");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("slow response → TIMEOUT")
    void timeout() {
        props.setTimeoutMs(300);
        mock.withDelay(1_500).respondWith(200, VALID);
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.TIMEOUT);
    }

    @Test
    @DisplayName("connection refused → NETWORK_ERROR")
    void connectionRefused() {
        int dead = mock.port();
        mock.close();
        props.setAnthropicBaseUrl("http://127.0.0.1:" + dead);
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.NETWORK_ERROR);
    }
}
