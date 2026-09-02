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
 * OpenAiCompatibleLlmProvider — HTTP client for any endpoint that speaks the
 * OpenAI {@code POST /chat/completions} contract: OpenAI itself, a local Ollama
 * ({@code http://localhost:11434/v1}), or a self-hosted gateway.
 *
 * <p>Uses the JDK's built-in {@link java.net.http.HttpClient} — no new
 * dependency, no reactive stack, explicit timeout, and easy to stub in tests
 * behind the {@link LlmProvider} seam.
 *
 * <p>Security:
 * <ul>
 *   <li>the API key only ever appears in the {@code Authorization} header of the
 *       outbound request — never logged, never in an exception message;</li>
 *   <li>on any non-2xx status the response body is discarded and a generic
 *       {@link LlmException} is thrown ({@code "provider returned HTTP 5xx"});</li>
 *   <li>no retry.</li>
 * </ul>
 */
@Slf4j
@Component
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /** Aliases this provider answers to, beyond its own {@link #id()}. */
    private static final java.util.Set<String> ALIASES =
            java.util.Set.of("openai", "ollama", "openai-compatible", "compatible", "custom");

    public OpenAiCompatibleLlmProvider(AiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(500, props.getConnectTimeoutMs())))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public boolean supports(String providerId) {
        return providerId != null && ALIASES.contains(providerId.trim().toLowerCase());
    }

    /**
     * Stable identity for this implementation. It covers the whole
     * OpenAI-chat-completions-compatible family (OpenAI, Ollama's {@code /v1},
     * self-hosted gateways) — which concrete endpoint is used is decided by
     * {@code app.ai.base-url} / {@code app.ai.provider}, not by this id. When it
     * is the only provider bean, {@link LlmClient} selects it regardless of the
     * configured {@code provider} name.
     */
    @Override
    public String id() {
        return "openai";
    }

    @Override
    public boolean isConfigured() {
        return props.isUsable();
    }

    @Override
    public LlmCompletion complete(LlmRequest request) {
        String url = chatCompletionsUrl();
        String payload = buildRequestBody(request);

        HttpRequest.Builder httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(Math.max(1_000, props.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            httpReq.header("Authorization", "Bearer " + props.getApiKey());
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpReq.build(), HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException | InterruptedException e) {
            throw LlmHttpSupport.fromTransportFailure("AI provider", e);
        }

        int status = response.statusCode();
        if (status / 100 != 2) {
            // Body deliberately NOT included — it can echo the prompt / keys.
            log.warn("AI provider returned non-2xx: status={}", status);
            throw new LlmException(LlmHttpSupport.reasonForStatus(status),
                    "AI provider returned HTTP " + status);
        }

        return new LlmCompletion(extractContent(response.body()), id(), props.getModel());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private String chatCompletionsUrl() {
        String base = props.getBaseUrl() == null ? "" : props.getBaseUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
    }

    private String buildRequestBody(LlmRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", props.getModel());
        root.put("temperature", request.temperature());
        root.put("max_tokens", request.maxOutputTokens());
        if (request.jsonObject()) {
            ObjectNode fmt = root.putObject("response_format");
            fmt.put("type", "json_object");
        }
        ArrayNode messages = root.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", request.systemPrompt());
        ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", request.userPrompt());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmException("Unable to build AI request", e);
        }
    }

    private String extractContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || !content.isTextual() || content.asText().isBlank()) {
                throw new LlmException(LlmFailureReason.INVALID_RESPONSE,
                        "AI provider returned an empty response");
            }
            return content.asText();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException(LlmFailureReason.INVALID_RESPONSE,
                    "AI provider returned an unreadable response", e);
        }
    }
}
