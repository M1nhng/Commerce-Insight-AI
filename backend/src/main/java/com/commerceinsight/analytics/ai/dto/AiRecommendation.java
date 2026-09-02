package com.commerceinsight.analytics.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AiRecommendation — a suggested action. Explicitly labelled as advice, never as
 * an observed fact. {@code priority} is one of LOW, MEDIUM, HIGH (validated and
 * normalised server-side).
 */
@Schema(description = "An AI-suggested action, clearly separate from observed facts")
public record AiRecommendation(
        String title,
        String description,
        String priority
) {}
