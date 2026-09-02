package com.commerceinsight.analytics.ai.llm;

import com.commerceinsight.analytics.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OPT-IN real-provider smoke test. Disabled by default and in CI — it runs ONLY
 * when {@code AI_REAL_PROVIDER_TEST=true} is set in the environment.
 *
 * <p>Reads the same env vars the app uses ({@code AI_PROVIDER}, {@code AI_MODEL},
 * {@code AI_API_KEY} / {@code OPENAI_API_KEY} / {@code ANTHROPIC_API_KEY},
 * {@code AI_BASE_URL}, {@code ANTHROPIC_BASE_URL}). Makes exactly ONE real
 * completion call and asserts a non-blank response.
 *
 * <p>Never prints the API key, the prompt, or the provider response. If the
 * provider is misconfigured the test fails with a generic assertion message —
 * no secret or body is surfaced.
 *
 * <pre>
 *   AI_REAL_PROVIDER_TEST=true AI_PROVIDER=openai AI_MODEL=gpt-4o-mini \
 *   OPENAI_API_KEY=sk-... ./mvnw -o test -Dtest=RealProviderManualTest
 * </pre>
 */
@DisplayName("RealProviderManualTest (opt-in)")
@EnabledIfEnvironmentVariable(named = "AI_REAL_PROVIDER_TEST", matches = "true")
class RealProviderManualTest {

    @Test
    @DisplayName("one real completion returns non-blank content")
    void oneRealCall() {
        AiProperties props = new AiProperties();
        props.setEnabled(true);
        props.setProvider(env("AI_PROVIDER", "openai"));
        props.setModel(env("AI_MODEL", "gpt-4o-mini"));
        props.setApiKey(firstNonBlank(
                System.getenv("AI_API_KEY"),
                System.getenv("OPENAI_API_KEY"),
                System.getenv("ANTHROPIC_API_KEY")));
        props.setBaseUrl(env("AI_BASE_URL", "https://api.openai.com/v1"));
        props.setAnthropicBaseUrl(env("ANTHROPIC_BASE_URL", "https://api.anthropic.com"));
        props.setTimeoutMs(30_000);
        props.setConnectTimeoutMs(5_000);
        props.setMaxOutputTokens(200);

        LlmProvider provider = "anthropic".equalsIgnoreCase(props.getProvider())
                ? new AnthropicLlmProvider(props, new ObjectMapper())
                : new OpenAiCompatibleLlmProvider(props, new ObjectMapper());

        assertThat(provider.isConfigured())
                .as("provider must be configured for the opt-in real test")
                .isTrue();

        LlmRequest request = new LlmRequest(
                "You are a JSON generator. Reply with only a JSON object.",
                "Return {\"ok\": true} and nothing else.",
                0.0, 100, true);

        LlmCompletion completion = provider.complete(request);

        assertThat(completion.content()).isNotBlank();
        assertThat(completion.provider()).isNotBlank();
        // Do NOT print completion.content() — keep provider output out of logs.
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
