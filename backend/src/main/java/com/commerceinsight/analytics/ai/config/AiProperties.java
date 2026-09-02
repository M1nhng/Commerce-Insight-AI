package com.commerceinsight.analytics.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AiProperties — binds {@code app.ai.*}.
 *
 * <p>Mirrors the {@code @ConfigurationProperties} pattern used by
 * {@link com.commerceinsight.config.RateLimitProperties},
 * {@code com.commerceinsight.export.config.ExportProperties} and
 * {@code com.commerceinsight.dataimport.config.ImportProperties}.
 *
 * <p>The AI insights feature is <strong>optional</strong>. If {@code enabled} is
 * {@code false}, or no usable provider credentials are present, the application
 * still starts normally — the feature simply reports itself unavailable
 * ({@code available:false}) and the dashboard keeps working.
 *
 * <p>No secret has a committed default: {@code api-key} defaults to empty.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** Master switch. When false the feature is unavailable regardless of keys. */
    private boolean enabled = false;

    /**
     * Logical provider id. {@code openai}, {@code ollama} and any
     * OpenAI-compatible gateway are served by {@code OpenAiCompatibleLlmProvider};
     * {@code anthropic} is served by {@code AnthropicLlmProvider}. An unknown
     * value → the feature reports itself unavailable (no startup failure).
     */
    private String provider = "openai";

    /** Model name passed straight to the provider (e.g. {@code gpt-4o-mini}). */
    private String model = "gpt-4o-mini";

    /** API key. Empty for keyless providers (e.g. a local Ollama). Never logged. */
    private String apiKey = "";

    /** Chat-completions base URL (no trailing {@code /chat/completions}). */
    private String baseUrl = "https://api.openai.com/v1";

    /** Whole-call (request) timeout in milliseconds. No automatic retry is ever performed. */
    private int timeoutMs = 20_000;

    /** TCP connect timeout in milliseconds. */
    private int connectTimeoutMs = 5_000;

    /**
     * Base URL for the {@code anthropic} provider's native Messages API. Kept
     * separate from {@code base-url} (which is the OpenAI-compatible chat URL) so
     * switching {@code provider} does not require rewriting both.
     */
    private String anthropicBaseUrl = "https://api.anthropic.com";

    /** Anthropic API version header value (their native API requires it). */
    private String anthropicVersion = "2023-06-01";

    /** Upper bound on model output tokens (cost control). */
    private int maxOutputTokens = 900;

    /** Sampling temperature. Low = more deterministic narrative. */
    private double temperature = 0.2;

    /** Maximum allowed analysis window, in days (request validation). */
    private int maxRangeDays = 365;

    /**
     * Hard ceiling on the serialized analytics-context string sent to the model
     * (token / cost / prompt-injection surface control). The context builder
     * only ever emits compact aggregates, so this is a backstop.
     */
    private int maxContextChars = 12_000;

    /** True when the feature is switched on AND a provider can actually be called. */
    public boolean isUsable() {
        if (!enabled) {
            return false;
        }
        boolean keyless = "ollama".equalsIgnoreCase(provider);
        return keyless || (apiKey != null && !apiKey.isBlank());
    }
}
