package com.commerceinsight.analytics.ai.llm;

/**
 * LlmException — any failure while talking to an LLM provider.
 *
 * <p>Deliberately opaque: the message is a short, user-safe phrase. Provider
 * response bodies, credentials, URLs and stack detail are never placed in the
 * message. {@link com.commerceinsight.analytics.ai.AiAnalyticsService} catches
 * this and degrades to an "unavailable" response — it is never propagated to the
 * HTTP layer.
 *
 * <p>Carries a {@link LlmFailureReason} (default {@link LlmFailureReason#PROVIDER_ERROR})
 * for safe logging and low-cardinality metrics tags.
 */
public class LlmException extends RuntimeException {

    private final LlmFailureReason reason;

    public LlmException(String message) {
        this(LlmFailureReason.PROVIDER_ERROR, message, null);
    }

    public LlmException(String message, Throwable cause) {
        this(LlmFailureReason.PROVIDER_ERROR, message, cause);
    }

    public LlmException(LlmFailureReason reason, String message) {
        this(reason, message, null);
    }

    public LlmException(LlmFailureReason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason == null ? LlmFailureReason.PROVIDER_ERROR : reason;
    }

    public LlmFailureReason getReason() {
        return reason;
    }
}
