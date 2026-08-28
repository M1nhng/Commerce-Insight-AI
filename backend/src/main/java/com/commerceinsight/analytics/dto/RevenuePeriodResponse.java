package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * RevenuePeriodResponse — one data point in a revenue time-series.
 *
 * <p>The {@code period} string is formatted by the chosen granularity:
 * <ul>
 *   <li>DAY   → "2026-08-01"</li>
 *   <li>WEEK  → "2026-W32"</li>
 *   <li>MONTH → "2026-08"</li>
 * </ul>
 */
@Schema(description = "A single data point in the revenue time-series")
public record RevenuePeriodResponse(

        @Schema(description = "Period label formatted by groupBy (e.g. '2026-08-01')")
        String period,

        @Schema(description = "Total revenue for this period (revenue-eligible orders only)")
        BigDecimal revenue,

        @Schema(description = "Number of revenue-eligible orders in this period")
        long orders
) {}
