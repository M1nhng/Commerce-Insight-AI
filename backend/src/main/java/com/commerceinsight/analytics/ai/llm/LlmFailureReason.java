package com.commerceinsight.analytics.ai.llm;

/**
 * LlmFailureReason — a small, provider-neutral taxonomy for why a completion
 * call failed. Every {@link LlmProvider} maps its own HTTP status codes /
 * exceptions onto these categories so the layers above (and the metrics tags)
 * never see provider-specific internals.
 *
 * <p>The API consumer never sees this either — a provider failure always
 * degrades to {@code available:false} with HTTP 200. It is used only for safe
 * logging and low-cardinality metrics tags.
 */
public enum LlmFailureReason {

    /** Feature disabled, no provider matched, or the matched provider has no key. */
    NOT_CONFIGURED,

    /** The request exceeded the configured request timeout. */
    TIMEOUT,

    /** Provider rejected the credentials (HTTP 401 / 403). */
    UNAUTHORIZED,

    /** Provider throttled the call (HTTP 429). */
    RATE_LIMITED,

    /** Provider returned a 5xx or another non-success status. */
    PROVIDER_ERROR,

    /** 2xx, but the body was missing / not JSON / had no usable content. */
    INVALID_RESPONSE,

    /** Could not reach the provider at all (DNS, connection refused, TLS, I/O). */
    NETWORK_ERROR
}
