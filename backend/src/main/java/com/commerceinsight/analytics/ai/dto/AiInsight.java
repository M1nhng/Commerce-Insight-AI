package com.commerceinsight.analytics.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AiInsight — one observation the model drew from the supplied analytics context.
 *
 * <p>{@code type} is one of POSITIVE, NEGATIVE, WARNING, OPPORTUNITY, TREND.
 * {@code severity} is one of LOW, MEDIUM, HIGH. Both are validated and
 * normalised server-side by
 * {@link com.commerceinsight.analytics.ai.AiAnalyticsResponseParser}; unknown
 * values are coerced to a safe default rather than passed through.
 *
 * <p>All fields are plain text and are rendered as plain text by the frontend.
 */
@Schema(description = "A single AI-generated insight derived only from the analytics context")
public record AiInsight(
        String type,
        String title,
        String description,
        String metric,
        String severity
) {}
