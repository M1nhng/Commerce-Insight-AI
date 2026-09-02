package com.commerceinsight.analytics.ai.llm;

/**
 * LlmHttpSupport — the tiny bit of HTTP-failure handling that every
 * {@link LlmProvider} implementation shares. Provider-specific request bodies
 * and response shapes stay inside each provider; only the safe status → reason
 * classification lives here so it stays consistent.
 *
 * <p>Response bodies are never passed in — they can echo the prompt or a key.
 */
final class LlmHttpSupport {

    private LlmHttpSupport() {
    }

    /** Map a non-2xx HTTP status onto a provider-neutral failure reason. */
    static LlmFailureReason reasonForStatus(int status) {
        if (status == 401 || status == 403) {
            return LlmFailureReason.UNAUTHORIZED;
        }
        if (status == 429) {
            return LlmFailureReason.RATE_LIMITED;
        }
        return LlmFailureReason.PROVIDER_ERROR;
    }

    /** Convert a low-level send() exception into a typed {@link LlmException}. */
    static LlmException fromTransportFailure(String providerLabel, Exception e) {
        if (e instanceof java.net.http.HttpTimeoutException) {
            return new LlmException(LlmFailureReason.TIMEOUT, providerLabel + " timed out", e);
        }
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return new LlmException(LlmFailureReason.NETWORK_ERROR, providerLabel + " request was interrupted", e);
        }
        // Any other IOException: DNS, connection refused, TLS, socket reset…
        return new LlmException(LlmFailureReason.NETWORK_ERROR, providerLabel + " is unreachable", e);
    }
}
