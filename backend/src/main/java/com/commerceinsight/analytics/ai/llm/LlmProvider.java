package com.commerceinsight.analytics.ai.llm;

/**
 * LlmProvider — the extension point for talking to a large-language-model
 * backend. One implementation ships today
 * ({@link OpenAiCompatibleLlmProvider}); adding Anthropic / Gemini / a local
 * gateway later means adding another {@code @Component} that implements this
 * interface — {@link LlmClient} and everything above it stay unchanged.
 *
 * <p>Implementations MUST:
 * <ul>
 *   <li>perform no automatic retries,</li>
 *   <li>enforce a bounded timeout,</li>
 *   <li>never log the API key, Authorization header, prompt bodies, or raw
 *       provider responses,</li>
 *   <li>throw {@link LlmException} (with a user-safe message) on any failure.</li>
 * </ul>
 */
public interface LlmProvider {

    /** Logical id this provider answers to (matches {@code app.ai.provider}). */
    String id();

    /**
     * Whether this provider serves the given {@code app.ai.provider} value.
     * Default: an exact (case-insensitive) match against {@link #id()}. A
     * provider that fronts a family of aliases (e.g. the OpenAI-compatible one
     * also serving {@code ollama}) overrides this.
     */
    default boolean supports(String providerId) {
        return providerId != null && providerId.trim().equalsIgnoreCase(id());
    }

    /** True when this provider has everything it needs to make a call. */
    boolean isConfigured();

    /**
     * Execute one stateless completion.
     *
     * @throws LlmException on timeout, transport failure, non-2xx status, or an
     *                      unparseable provider payload
     */
    LlmCompletion complete(LlmRequest request);
}
