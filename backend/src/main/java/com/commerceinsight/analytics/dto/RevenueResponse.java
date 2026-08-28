package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * RevenueResponse — revenue time-series with metadata.
 */
@Schema(description = "Revenue time-series grouped by DAY, WEEK, or MONTH")
public record RevenueResponse(

        @Schema(description = "Grouping granularity applied: DAY, WEEK, or MONTH")
        String groupBy,

        @Schema(description = "ISO 4217 currency code for all monetary values")
        String currency,

        @Schema(description = "Start of the reporting window (null = all-time)")
        Instant dateFrom,

        @Schema(description = "End of the reporting window (null = now)")
        Instant dateTo,

        @Schema(description = "Time-series data points, ordered chronologically")
        List<RevenuePeriodResponse> data
) {}
