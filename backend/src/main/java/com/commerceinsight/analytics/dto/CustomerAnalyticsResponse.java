package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * CustomerAnalyticsResponse — customer engagement metrics for a time window.
 *
 * <p>Definitions:
 * <ul>
 *   <li><b>uniqueCustomers</b> — distinct customer_ids on orders in the period</li>
 *   <li><b>newCustomers</b> — customers whose FIRST-EVER order falls in the period</li>
 *   <li><b>repeatCustomers</b> — customers with &gt;1 order in the period</li>
 *   <li><b>averageOrdersPerCustomer</b> — totalOrders / uniqueCustomers (0 if no customers)</li>
 * </ul>
 */
@Schema(description = "Customer engagement metrics for a given time window")
public record CustomerAnalyticsResponse(

        @Schema(description = "Number of distinct customers who placed at least one order in the period")
        long uniqueCustomers,

        @Schema(description = "Customers whose very first order was placed in this period")
        long newCustomers,

        @Schema(description = "Customers who placed more than one order in this period")
        long repeatCustomers,

        @Schema(description = "Average number of orders per unique customer (0 when no customers)")
        BigDecimal averageOrdersPerCustomer,

        @Schema(description = "Start of the reporting window (null = all-time)")
        Instant dateFrom,

        @Schema(description = "End of the reporting window (null = now)")
        Instant dateTo
) {}
