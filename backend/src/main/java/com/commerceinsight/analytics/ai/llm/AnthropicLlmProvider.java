package com.commerceinsight.analytics.ai.llm;

import com.commerceinsight.analytics.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AnthropicLlmProvider — native Claude Messages API client
 * ({@code POST {anthropic-base-url}/v1/messages}).
 *
 * <p>Uses the JDK {@link java.net.http.HttpClient} — no Anthropic SDK, no new
 * dependency. Same guarantees as every {@link LlmProvider}: bounded timeout, no
 * retry, no key / prompt / response body in logs or exception messages; all
 * failures become a typed {@link LlmException}.
 *
 * <p>Wire-format specifics that stay inside this class:
 * <ul>
 *   <li>headers {@code x-api-key} + {@code anthropic-version} (not a Bearer token);</li>
 *   <li>the system prompt is a top-level {@code system} field, not a message;</li>
 *   <li>to force a bare JSON object the assistant turn is pre-filled with
 *       {@code "{"} and that brace is prepended back onto the returned text.</li>
 * </ul>
 */
@Slf4j
@Component
public class AnthropicLlmProvider implements LlmProvider {

    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AnthropicLlmProvider(AiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, props.getConnectTimeoutMs())))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public String id() {
        return "anthropic";
    }

    @Override
    public boolean isConfigured() {
        // Anthropic always needs a key; AiProperties.isUsable() already requires
        // one for any non-"ollama" provider.
        return props.isUsable() && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    @Override
    public LlmCompletion complete(LlmRequest request) {
        String url = messagesUrl();
        String payload = buildRequestBody(request);

        HttpRequest.Builder httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(1_000, props.getTimeoutMs())))
                .header("content-type", "application/json")
                .header("anthropic-version", props.getAnthropicVersion())
                .header("x-api-key", props.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpReq.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException | InterruptedException e) {
            throw LlmHttpSupport.fromTransportFailure("Anthropic", e);
        }

        int status = response.statusCode();
        if (status / 100 != 2) {
            log.warn("Anthropic returned non-2xx: status={}", status);
            throw new LlmException(LlmHttpSupport.reasonForStatus(status),
                    "AI provider returned HTTP " + status);
        }

        return new LlmCompletion(extractContent(request, response.body()), id(), props.getModel());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private String messagesUrl() {
        String base = props.getAnthropicBaseUrl() == null ? "" : props.getAnthropicBaseUrl().trim();
        if (base.isEmpty()) {
            base = "https://api.anthropic.com";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base.endsWith("/v1/messages") ? base : base + "/v1/messages";
    }

    private String buildRequestBody(LlmRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", props.getModel());
        root.put("max_tokens", request.maxOutputTokens());
        root.put("temperature", request.temperature());
        root.put("system", request.systemPrompt());

        ArrayNode messages = root.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", request.userPrompt());
        if (request.jsonObject()) {
            // Assistant prefill: Claude continues from an opening brace, so the
            // reply is a bare JSON object with no "Here is the JSON:" preamble.
            ObjectNode assistant = messages.addObject();
            assistant.put("role", "assistant");
            assistant.put("content", "{");
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("Unable to build AI request", e);
        }
    }

    private String extractContent(LlmRequest request, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode blocks = root.path("content");
            String text = null;
            if (blocks.isArray()) {
                for (JsonNode block : blocks) {
                    if ("text".equals(block.path("type").asText()) && block.path("text").isTextual()) {
                        text = block.path("text").asText();
                        break;
                    }
                }
            }
            if (text == null || text.isBlank()) {
                throw new LlmException(LlmFailureReason.INVALID_RESPONSE,
                        "AI provider returned an empty response");
            }
            // Re-attach the prefilled opening brace so the parser sees full JSON.
            return request.jsonObject() && !text.trim().startsWith("{") ? "{" + text : text;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException(LlmFailureReason.INVALID_RESPONSE,
                    "AI provider returned an unreadable response", e);
        }
    }
}
