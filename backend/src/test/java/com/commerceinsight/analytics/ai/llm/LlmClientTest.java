package com.commerceinsight.analytics.ai.llm;

import com.commerceinsight.analytics.ai.config.AiProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LlmClient} — provider selection by {@code app.ai.provider}
 * (including aliases via {@code supports()}), safe behaviour for an unknown
 * provider, and that nothing here crashes on startup.
 */
@DisplayName("LlmClient")
class LlmClientTest {

    /** A no-network fake standing in for a provider. */
    private static final class FakeProvider implements LlmProvider {
        private final String id;
        private final boolean configured;
        private final java.util.Set<String> aliases;

        FakeProvider(String id, boolean configured, String... aliases) {
            this.id = id;
            this.configured = configured;
            this.aliases = new java.util.HashSet<>(java.util.List.of(aliases));
            this.aliases.add(id);
        }

        @Override public String id() { return id; }
        @Override public boolean supports(String p) {
            return p != null && aliases.contains(p.trim().toLowerCase());
        }
        @Override public boolean isConfigured() { return configured; }
        @Override public LlmCompletion complete(LlmRequest request) {
            return new LlmCompletion("{\"summary\":\"ok\"}", id, "model-x");
        }
    }

    private static AiProperties props(String provider, boolean enabled) {
        AiProperties p = new AiProperties();
        p.setEnabled(enabled);
        p.setProvider(provider);
        p.setApiKey("k");
        return p;
    }

    @Test
    @DisplayName("provider=openai → OpenAI-compatible bean serves it")
    void selectsOpenAi() {
        LlmClient client = new LlmClient(props("openai", true), List.of(
                new FakeProvider("openai", true, "ollama", "openai-compatible"),
                new FakeProvider("anthropic", true)));
        assertThat(client.isAvailable()).isTrue();
        assertThat(client.activeProviderId()).isEqualTo("openai");
        assertThat(client.complete(new LlmRequest("s", "u", 0.2, 900, true)).provider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("provider=ollama → still served by the OpenAI-compatible bean (alias)")
    void selectsOllamaAlias() {
        LlmClient client = new LlmClient(props("ollama", true), List.of(
                new FakeProvider("openai", true, "ollama", "openai-compatible"),
                new FakeProvider("anthropic", true)));
        assertThat(client.isAvailable()).isTrue();
        assertThat(client.activeProviderId()).isEqualTo("openai");
    }

    @Test
    @DisplayName("provider=anthropic → Anthropic bean")
    void selectsAnthropic() {
        LlmClient client = new LlmClient(props("anthropic", true), List.of(
                new FakeProvider("openai", true, "ollama"),
                new FakeProvider("anthropic", true)));
        assertThat(client.activeProviderId()).isEqualTo("anthropic");
        assertThat(client.complete(new LlmRequest("s", "u", 0.2, 900, true)).provider()).isEqualTo("anthropic");
    }

    @Test
    @DisplayName("unknown provider + multiple beans → not available, complete() throws NOT_CONFIGURED, no crash")
    void unknownProvider() {
        LlmClient client = new LlmClient(props("gemini", true), List.of(
                new FakeProvider("openai", true, "ollama"),
                new FakeProvider("anthropic", true)));
        assertThat(client.isAvailable()).isFalse();
        assertThat(client.activeProviderId()).isEqualTo("none");
        assertThatThrownBy(() -> client.complete(new LlmRequest("s", "u", 0.2, 900, true)))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.NOT_CONFIGURED);
    }

    @Test
    @DisplayName("feature disabled → not available regardless of a matching provider")
    void disabled() {
        LlmClient client = new LlmClient(props("openai", false), List.of(new FakeProvider("openai", true)));
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("matched provider present but not configured → not available")
    void matchedButUnconfigured() {
        LlmClient client = new LlmClient(props("anthropic", true), List.of(
                new FakeProvider("openai", true),
                new FakeProvider("anthropic", false)));
        assertThat(client.isAvailable()).isFalse();
        assertThatThrownBy(() -> client.complete(new LlmRequest("s", "u", 0.2, 900, true)))
                .isInstanceOf(LlmException.class)
                .hasFieldOrPropertyWithValue("reason", LlmFailureReason.NOT_CONFIGURED);
    }

    @Test
    @DisplayName("single provider bean → used even if the configured name does not match (dev fallback)")
    void singleProviderFallback() {
        LlmClient client = new LlmClient(props("whatever", true), List.of(new FakeProvider("openai", true)));
        assertThat(client.isAvailable()).isTrue();
        assertThat(client.activeProviderId()).isEqualTo("openai");
    }
}
