package com.commerceinsight.analytics.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AiAnalyticsContext — the compact, aggregate-only snapshot handed to the LLM.
 *
 * <p>This is an <strong>internal</strong> type: it is never returned from a
 * controller and never serialised into an {@code ApiResponse}. It is serialised
 * (by {@link AiAnalyticsPromptBuilder}) into the user prompt as JSON.
 *
 * <p>Data-minimisation rules baked into the shape:
 * <ul>
 *   <li>only counts / sums / rates — no order rows, no line items, no customer
 *       rows;</li>
 *   <li>no customer email / phone / address / id, no user id, no JWT, no keys;</li>
 *   <li>the only free-text fields are product name + SKU (needed to answer
 *       "which products drive revenue") — the prompt marks them untrusted.</li>
 * </ul>
 */
public record AiAnalyticsContext(
        Window window,
        String currency,
        Overview overview,
        List<RevenuePoint> revenueByMonth,
        Growth growth,
        Map<String, Long> ordersByStatus,
        List<TopProduct> topProducts,
        Map<String, PaymentMethod> paymentMethods,
        Customers customers,
        Inventory inventory
) {

    public record Window(String dateFrom, String dateTo, long days) {}

    public record Overview(
            BigDecimal totalRevenue,
            long totalOrders,
            long revenueEligibleOrders,
            long uniqueCustomers,
            long productsSold,
            long cancelledOrders,
            BigDecimal cancellationRate,
            BigDecimal averageOrderValue
    ) {}

    public record RevenuePoint(String period, BigDecimal revenue, long orders) {}

    public record Growth(
            BigDecimal previousPeriodRevenue,
            BigDecimal revenueGrowthPct,
            String note
    ) {}

    public record TopProduct(String productName, String sku, long quantitySold, BigDecimal revenue) {}

    public record PaymentMethod(long orders, BigDecimal amount) {}

    public record Customers(
            long uniqueCustomers,
            long newCustomers,
            long repeatCustomers,
            BigDecimal averageOrdersPerCustomer
    ) {}

    public record Inventory(long lowStockItems, long outOfStockItems) {}
}
