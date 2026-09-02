package com.commerceinsight.analytics.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * AiInsightsResponse — the stable, provider-agnostic contract returned to the
 * dashboard and the MCP tool. Raw provider payloads never reach this shape.
 *
 * <p>{@code available == false} is the safe degraded form used whenever the
 * feature is switched off, unconfigured, or the provider call failed / returned
 * something unusable. In that case {@code insights} and {@code recommendations}
 * are empty and {@code provider}/{@code model} are {@code null}.
 */
@Schema(description = "AI-generated ecommerce insights over the existing analytics data")
public record AiInsightsResponse(

        @Schema(description = "False when AI is disabled/unconfigured/unavailable — the dashboard still works")
        boolean available,

        @Schema(description = "Short natural-language summary of the period")
        String summary,

        List<AiInsight> insights,

        List<AiRecommendation> recommendations,

        @Schema(description = "When this response was produced (server clock)")
        Instant generatedAt,

        @Schema(description = "Logical provider id that served the request; null when unavailable")
        String provider,

        @Schema(description = "Model name; null when unavailable")
        String model
) {

    /** The safe degraded response — dashboard keeps working, AI section shows "unavailable". */
    public static AiInsightsResponse unavailable() {
        return new AiInsightsResponse(
                false,
                "AI insights are temporarily unavailable.",
                List.of(),
                List.of(),
                Instant.now(),
                null,
                null
        );
    }

    /** A successful, already-validated response. */
    public static AiInsightsResponse of(String summary,
                                        List<AiInsight> insights,
                                        List<AiRecommendation> recommendations,
                                        String provider,
                                        String model) {
        return new AiInsightsResponse(
                true,
                summary,
                insights,
                recommendations,
                Instant.now(),
                provider,
                model
        );
    }
}
