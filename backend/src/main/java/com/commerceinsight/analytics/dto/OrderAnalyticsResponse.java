package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OrderAnalyticsResponse — breakdown of orders by status with derived rates.
 *
 * <p>Division-by-zero is prevented in the service:
 * rates are 0.00 when {@code totalOrders} is 0.
 */
@Schema(description = "Order count breakdown by status with completion and cancellation rates")
public record OrderAnalyticsResponse(

        @Schema(description = "Total orders created in the period (all statuses)")
        long totalOrders,

        @Schema(description = "Orders currently in PENDING status")
        long pendingOrders,

        @Schema(description = "Orders currently in CONFIRMED status")
        long confirmedOrders,

        @Schema(description = "Orders currently in PROCESSING status")
        long processingOrders,

        @Schema(description = "Orders currently in SHIPPED status")
        long shippedOrders,

        @Schema(description = "Orders currently in DELIVERED status")
        long deliveredOrders,

        @Schema(description = "Orders currently in COMPLETED status (terminal success)")
        long completedOrders,

        @Schema(description = "Orders currently in CANCELLED status (terminal failure)")
        long cancelledOrders,

        @Schema(description = "Completion rate as percentage of total orders (0.00–100.00)")
        BigDecimal completionRate,

        @Schema(description = "Cancellation rate as percentage of total orders (0.00–100.00)")
        BigDecimal cancellationRate,

        @Schema(description = "Start of the reporting window (null = all-time)")
        Instant dateFrom,

        @Schema(description = "End of the reporting window (null = now)")
        Instant dateTo
) {}
