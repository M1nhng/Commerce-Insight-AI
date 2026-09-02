package com.commerceinsight.analytics.ai.llm;

/**
 * LlmCompletion — the useful part of a provider response.
 *
 * @param content  raw assistant message text (expected to be a JSON object here)
 * @param provider logical provider id that served the request
 * @param model    model name the provider reported / was asked for
 */
public record LlmCompletion(String content, String provider, String model) {}
