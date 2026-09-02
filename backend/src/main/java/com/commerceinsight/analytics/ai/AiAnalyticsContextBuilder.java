package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.dto.CustomerAnalyticsResponse;
import com.commerceinsight.analytics.dto.OrderAnalyticsResponse;
import com.commerceinsight.analytics.dto.OverviewResponse;
import com.commerceinsight.analytics.dto.PaymentAnalyticsResponse;
import com.commerceinsight.analytics.dto.PaymentMethodStats;
import com.commerceinsight.analytics.dto.RevenueResponse;
import com.commerceinsight.analytics.dto.TopProductEntry;
import com.commerceinsight.analytics.service.AnalyticsService;
import com.commerceinsight.inventory.dto.response.InventoryResponse;
import com.commerceinsight.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AiAnalyticsContextBuilder — a thin adapter that composes the EXISTING
 * {@link AnalyticsService} (and {@link InventoryService} for a risk signal) into
 * one compact {@link AiAnalyticsContext}. It runs no SQL of its own and defines
 * no revenue / status semantics — those stay in {@code AnalyticsRepository}.
 *
 * <p>One extra call is made for the immediately-preceding, equal-length window
 * so the model has a baseline for "why did revenue change" questions — this is
 * still just {@code AnalyticsService.getOverview}, not a new query.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAnalyticsContextBuilder {

    private static final int TOP_PRODUCT_LIMIT = 10;

    private final AnalyticsService analyticsService;
    private final InventoryService inventoryService;

    public AiAnalyticsContext build(Instant dateFrom, Instant dateTo) {
        OverviewResponse overview = analyticsService.getOverview(dateFrom, dateTo);
        OrderAnalyticsResponse orders = analyticsService.getOrderAnalytics(dateFrom, dateTo);
        RevenueResponse revenue = analyticsService.getRevenue(dateFrom, dateTo, "MONTH");
        List<TopProductEntry> topProducts = analyticsService.getTopProducts(dateFrom, dateTo, TOP_PRODUCT_LIMIT);
        CustomerAnalyticsResponse customers = analyticsService.getCustomerAnalytics(dateFrom, dateTo);
        PaymentAnalyticsResponse payments = analyticsService.getPaymentAnalytics(dateFrom, dateTo);

        long days = Math.max(1, Duration.between(dateFrom, dateTo).toDays());
        AiAnalyticsContext.Growth growth = buildGrowth(dateFrom, dateTo, days, overview.totalRevenue());

        long revenueEligible = orders.confirmedOrders() + orders.processingOrders()
                + orders.shippedOrders() + orders.deliveredOrders() + orders.completedOrders();

        return new AiAnalyticsContext(
                new AiAnalyticsContext.Window(dateFrom.toString(), dateTo.toString(), days),
                overview.currency(),
                new AiAnalyticsContext.Overview(
                        nz(overview.totalRevenue()),
                        overview.totalOrders(),
                        revenueEligible,
                        overview.totalCustomers(),
                        overview.totalProductsSold(),
                        overview.cancelledOrders(),
                        nz(overview.cancellationRate()),
                        nz(overview.averageOrderValue())
                ),
                revenue.data().stream()
                        .map(p -> new AiAnalyticsContext.RevenuePoint(p.period(), nz(p.revenue()), p.orders()))
                        .toList(),
                growth,
                statusMap(orders),
                topProducts.stream()
                        .map(tp -> new AiAnalyticsContext.TopProduct(
                                tp.productName(), tp.sku(), tp.quantitySold(), nz(tp.revenue())))
                        .toList(),
                paymentMap(payments),
                new AiAnalyticsContext.Customers(
                        customers.uniqueCustomers(),
                        customers.newCustomers(),
                        customers.repeatCustomers(),
                        nz(customers.averageOrdersPerCustomer())
                ),
                inventorySummary()
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private AiAnalyticsContext.Growth buildGrowth(Instant from, Instant to, long days, BigDecimal currentRevenue) {
        try {
            Instant prevTo = from;
            Instant prevFrom = from.minus(Duration.ofDays(days));
            BigDecimal prevRevenue = nz(analyticsService.getOverview(prevFrom, prevTo).totalRevenue());
            BigDecimal current = nz(currentRevenue);
            BigDecimal growthPct;
            String note;
            if (prevRevenue.signum() == 0) {
                growthPct = null;
                note = "No revenue in the preceding equal-length window; growth percentage is not defined.";
            } else {
                growthPct = current.subtract(prevRevenue)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(prevRevenue, 2, RoundingMode.HALF_UP);
                note = "revenueGrowthPct compares this window against the immediately preceding "
                        + days + "-day window.";
            }
            return new AiAnalyticsContext.Growth(prevRevenue, growthPct, note);
        } catch (RuntimeException e) {
            log.debug("AI context: previous-period revenue unavailable: {}", e.getMessage());
            return new AiAnalyticsContext.Growth(null, null,
                    "Previous-period baseline could not be computed.");
        }
    }

    private static Map<String, Long> statusMap(OrderAnalyticsResponse o) {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("PENDING", o.pendingOrders());
        m.put("CONFIRMED", o.confirmedOrders());
        m.put("PROCESSING", o.processingOrders());
        m.put("SHIPPED", o.shippedOrders());
        m.put("DELIVERED", o.deliveredOrders());
        m.put("COMPLETED", o.completedOrders());
        m.put("CANCELLED", o.cancelledOrders());
        return m;
    }

    private static Map<String, AiAnalyticsContext.PaymentMethod> paymentMap(PaymentAnalyticsResponse p) {
        Map<String, AiAnalyticsContext.PaymentMethod> m = new LinkedHashMap<>();
        Map<String, PaymentMethodStats> breakdown = p.breakdown();
        if (breakdown != null) {
            breakdown.forEach((k, v) ->
                    m.put(k, new AiAnalyticsContext.PaymentMethod(v.orders(), nz(v.amount()))));
        }
        return m;
    }

    private AiAnalyticsContext.Inventory inventorySummary() {
        try {
            List<InventoryResponse> low = inventoryService.findLowStock();
            long out = low.stream().filter(i -> i.availableQuantity() <= 0).count();
            return new AiAnalyticsContext.Inventory(low.size(), out);
        } catch (RuntimeException e) {
            log.debug("AI context: inventory summary unavailable: {}", e.getMessage());
            return new AiAnalyticsContext.Inventory(0, 0);
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
