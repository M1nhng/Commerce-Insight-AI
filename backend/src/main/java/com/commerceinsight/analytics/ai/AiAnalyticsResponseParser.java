package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.dto.AiInsight;
import com.commerceinsight.analytics.ai.dto.AiInsightsResponse;
import com.commerceinsight.analytics.ai.dto.AiRecommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AiAnalyticsResponseParser — turns the raw model output string into a
 * validated, bounded {@link AiInsightsResponse}. Raw LLM output is never
 * trusted:
 *
 * <ul>
 *   <li>if it is not a JSON object, or the summary is empty → degrade to
 *       {@link AiInsightsResponse#unavailable()};</li>
 *   <li>enum fields are coerced to the allowed set (unknown → safe default);</li>
 *   <li>at most {@value #MAX_INSIGHTS} insights and {@value #MAX_RECOMMENDATIONS}
 *       recommendations are kept;</li>
 *   <li>strings are whitespace-collapsed and length-capped.</li>
 * </ul>
 *
 * The result is plain text only; the frontend renders it without any HTML sink.
 */
@Slf4j
@Component
public class AiAnalyticsResponseParser {

    static final int MAX_INSIGHTS = 5;
    static final int MAX_RECOMMENDATIONS = 5;
    static final int MAX_SUMMARY = 600;
    static final int MAX_TITLE = 200;
    static final int MAX_DESCRIPTION = 1000;
    static final int MAX_METRIC = 200;

    private static final Set<String> INSIGHT_TYPES =
            Set.of("POSITIVE", "NEGATIVE", "WARNING", "OPPORTUNITY", "TREND");
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH");

    private final ObjectMapper objectMapper;

    public AiAnalyticsResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param rawModelOutput assistant message content (expected: one JSON object)
     * @param provider       provider id to stamp on a successful response
     * @param model          model name to stamp on a successful response
     */
    public AiInsightsResponse parse(String rawModelOutput, String provider, String model) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawModelOutput == null ? "" : rawModelOutput.trim());
        } catch (Exception e) {
            log.warn("AI response is not valid JSON — degrading to unavailable");
            return AiInsightsResponse.unavailable();
        }
        if (root == null || !root.isObject()) {
            log.warn("AI response is not a JSON object — degrading to unavailable");
            return AiInsightsResponse.unavailable();
        }

        String summary = cap(text(root.path("summary")), MAX_SUMMARY);
        if (summary.isBlank()) {
            log.warn("AI response has no summary — degrading to unavailable");
            return AiInsightsResponse.unavailable();
        }

        List<AiInsight> insights = new ArrayList<>();
        for (JsonNode n : arrayOf(root, "insights")) {
            if (insights.size() >= MAX_INSIGHTS) break;
            String title = cap(text(n.path("title")), MAX_TITLE);
            String description = cap(text(n.path("description")), MAX_DESCRIPTION);
            if (title.isBlank() && description.isBlank()) continue;
            insights.add(new AiInsight(
                    coerce(text(n.path("type")).toUpperCase(), INSIGHT_TYPES, "TREND"),
                    title,
                    description,
                    cap(text(n.path("metric")), MAX_METRIC),
                    coerce(text(n.path("severity")).toUpperCase(), SEVERITIES, "LOW")
            ));
        }

        List<AiRecommendation> recommendations = new ArrayList<>();
        for (JsonNode n : arrayOf(root, "recommendations")) {
            if (recommendations.size() >= MAX_RECOMMENDATIONS) break;
            String title = cap(text(n.path("title")), MAX_TITLE);
            String description = cap(text(n.path("description")), MAX_DESCRIPTION);
            if (title.isBlank() && description.isBlank()) continue;
            recommendations.add(new AiRecommendation(
                    title,
                    description,
                    coerce(text(n.path("priority")).toUpperCase(), PRIORITIES, "MEDIUM")
            ));
        }

        return AiInsightsResponse.of(summary, List.copyOf(insights), List.copyOf(recommendations), provider, model);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static Iterable<JsonNode> arrayOf(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isArray() ? node : List.of();
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return "";
        String raw = node.isValueNode() ? node.asText() : node.toString();
        return raw.replaceAll("\\s+", " ").trim();
    }

    private static String cap(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max).trim();
    }

    private static String coerce(String value, Set<String> allowed, String fallback) {
        return allowed.contains(value) ? value : fallback;
    }
}
