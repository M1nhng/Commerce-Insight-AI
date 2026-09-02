package com.commerceinsight.analytics.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * AiInsightsRequest — body of {@code POST /api/v1/analytics/ai-insights}.
 *
 * <p>Unlike the read-only analytics GET endpoints (where an omitted bound means
 * "all-time"), both bounds are required here: an unbounded window would make the
 * analytics context — and therefore the LLM call cost and prompt size —
 * unpredictable. Cross-field rules (from &lt; to, span &le;
 * {@code app.ai.max-range-days}) are enforced in
 * {@link com.commerceinsight.analytics.ai.AiAnalyticsService}.
 */
public record AiInsightsRequest(

        @Schema(description = "Start of the analysis window (ISO 8601 instant)",
                example = "2026-01-01T00:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "dateFrom is required")
        Instant dateFrom,

        @Schema(description = "End of the analysis window (ISO 8601 instant)",
                example = "2026-09-01T00:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "dateTo is required")
        Instant dateTo
) {}
