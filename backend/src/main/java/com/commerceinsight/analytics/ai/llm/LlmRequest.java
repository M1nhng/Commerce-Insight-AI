package com.commerceinsight.analytics.ai.llm;

/**
 * LlmRequest — a single, stateless chat completion request.
 *
 * <p>Two messages only: a fixed system prompt and a user prompt that already
 * contains the compact analytics context. No conversation history, no tools,
 * no streaming — this feature is not a chatbot.
 *
 * @param systemPrompt   deterministic instruction block (roles + safety rules)
 * @param userPrompt     analytics context + analysis instruction
 * @param temperature    sampling temperature
 * @param maxOutputTokens hard cap on the model's response length
 * @param jsonObject     when true, ask the provider for a JSON object response
 */
public record LlmRequest(
        String systemPrompt,
        String userPrompt,
        double temperature,
        int maxOutputTokens,
        boolean jsonObject
) {}
