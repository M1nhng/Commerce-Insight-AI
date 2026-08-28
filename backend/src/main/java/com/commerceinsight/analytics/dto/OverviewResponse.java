package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * OverviewResponse — high-level KPI snapshot for the analytics dashboard.
 *
 * <p>Revenue figures include only orders with status CONFIRMED, PROCESSING,
 * SHIPPED, DELIVERED, or COMPLETED. PENDING, CANCELLED, and REFUNDED orders
 * are excluded from revenue totals.
 */
@Schema(description = "High-level ecommerce KPIs for a given time window")
public record OverviewResponse(

        @Schema(description = "Sum of total for revenue-eligible orders in period")
        BigDecimal totalRevenue,

        @Schema(description = "Count of all orders created in period (all statuses)")
        long totalOrders,

        @Schema(description = "Count of distinct customers who placed at least one order in period")
        long totalCustomers,

        @Schema(description = "Sum of item quantities sold across revenue-eligible orders in period")
        long totalProductsSold,

        @Schema(description = "Average order value across revenue-eligible orders (0 when no orders)")
        BigDecimal averageOrderValue,

        @Schema(description = "Count of orders with status CANCELLED in period")
        long cancelledOrders,

        @Schema(description = "Cancellation rate as a percentage of total orders (0.00–100.00)")
        BigDecimal cancellationRate,

        @Schema(description = "ISO 4217 currency code for all monetary values in this response")
        String currency,

        @Schema(description = "Start of the reporting window (inclusive). Null means all-time.")
        Instant dateFrom,

        @Schema(description = "End of the reporting window (inclusive). Null means now.")
        Instant dateTo
) {}
