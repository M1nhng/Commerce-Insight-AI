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
 * Real-HTTP-path tests for {@link OpenAiCompatibleLlmProvider} against a local
 * {@link MockLlmHttpServer}. No external API, no network. Covers request shape,
 * the Ollama (keyless) path, and every failure → {@link LlmFailureReason}.
 */
@DisplayName("OpenAiCompatibleLlmProvider")
class OpenAiCompatibleLlmProviderTest {

    private static final String VALID = """
            {"choices":[{"message":{"role":"assistant","content":"{\\"summary\\":\\"ok\\"}"}}]}""";

    private MockLlmHttpServer mock;
    private AiProperties props;

    @BeforeEach
    void setUp() throws Exception {
        mock = MockLlmHttpServer.start();
        props = new AiProperties();
        props.setEnabled(true);
        props.setProvider("openai");
        props.setModel("gpt-4o-mini");
        props.setApiKey("test-key");
        props.setBaseUrl(mock.baseUrl() + "/v1");
        props.setTimeoutMs(2_000);
        props.setConnectTimeoutMs(1_000);
    }

    @AfterEach
    void tearDown() {
        mock.close();
    }

    private OpenAiCompatibleLlmProvider provider() {
        return new OpenAiCompatibleLlmProvider(props, new ObjectMapper());
    }

    private LlmRequest req() {
        return new LlmRequest("SYSTEM RULES", "DATA CONTEXT + TASK", 0.2, 900, true);
    }

    @Test
    @DisplayName("valid 200 → LlmCompletion with the model text; correct endpoint, auth, body")
    void valid() {
        mock.respondWith(200, VALID);

        LlmCompletion c = provider().complete(req());

        assertThat(c.content()).contains("\"summary\":\"ok\"");
        assertThat(c.provider()).isEqualTo("openai");
        assertThat(c.model()).isEqualTo("gpt-4o-mini");
        assertThat(mock.lastPath.get()).isEqualTo("/v1/chat/completions");
        assertThat(mock.lastHeaders.get("authorization")).isEqualTo("Bearer test-key");
        assertThat(mock.lastBody.get()).contains("\"role\":\"system\"").contains("\"role\":\"user\"")
                .contains("\"response_format\"").contains("SYSTEM RULES");
    }

    @Test
    @DisplayName("provider=ollama, empty key → NO Authorization header")
    void ollamaKeyless() {
        props.setProvider("ollama");
        props.setApiKey("");
        mock.respondWith(200, VALID);

        provider().complete(req());

        assertThat(mock.lastHeaders).doesNotContainKey("authorization");
    }

    @Test
    @DisplayName("supports() covers the OpenAI-compatible family, not anthropic")
    void supports() {
        OpenAiCompatibleLlmProvider p = provider();
        assertThat(p.supports("openai")).isTrue();
        assertThat(p.supports("ollama")).isTrue();
        assertThat(p.supports("openai-compatible")).isTrue();
        assertThat(p.supports("anthropic")).isFalse();
        assertThat(p.supports(null)).isFalse();
    }

    @Test
    @DisplayName("401 → UNAUTHORIZED, no body leak")
    void unauthorized() {
        mock.respondWith(401, "{\"error\":{\"message\":\"bad key sk-secret\"}}");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.UNAUTHORIZED)
                .extracting(Throwable::getMessage).asString().doesNotContain("sk-secret");
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
        mock.respondWith(500, "boom");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.PROVIDER_ERROR);
    }

    @Test
    @DisplayName("200 but malformed JSON → INVALID_RESPONSE")
    void malformedJson() {
        mock.respondWith(200, "{ this is not json ");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("200 but empty content → INVALID_RESPONSE")
    void emptyContent() {
        mock.respondWith(200, "{\"choices\":[{\"message\":{\"content\":\"\"}}]}");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("slow response beyond timeout-ms → TIMEOUT")
    void timeout() {
        props.setTimeoutMs(300);
        mock.withDelay(1_500).respondWith(200, VALID);
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.TIMEOUT);
    }

    @Test
    @DisplayName("connection refused (dead port) → NETWORK_ERROR")
    void connectionRefused() {
        int dead = mock.port();
        mock.close(); // nothing is listening now
        props.setBaseUrl("http://127.0.0.1:" + dead + "/v1");
        assertThatThrownBy(() -> provider().complete(req()))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.NETWORK_ERROR);
    }
}
