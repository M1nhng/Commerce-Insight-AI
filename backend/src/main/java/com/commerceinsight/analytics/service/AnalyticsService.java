package com.commerceinsight.analytics.service;

import com.commerceinsight.analytics.dto.*;
import com.commerceinsight.analytics.repository.AnalyticsRepository;
import com.commerceinsight.analytics.repository.projection.CustomerOrderCountRow;
import com.commerceinsight.analytics.repository.projection.PaymentMethodRow;
import com.commerceinsight.analytics.repository.projection.RevenueTimeSeriesRow;
import com.commerceinsight.analytics.repository.projection.StatusCountRow;
import com.commerceinsight.analytics.repository.projection.TopProductRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AnalyticsService — read-only analytics aggregation.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>All data comes from {@link AnalyticsRepository} via PostgreSQL aggregation.</li>
 *   <li>No Order/Customer/Payment entities are loaded into Java memory.</li>
 *   <li>Division-by-zero is prevented by checking denominators before dividing.</li>
 *   <li>Currency is included in responses — never assumed to be USD.</li>
 * </ul>
 *
 * <p>Revenue-eligible statuses: CONFIRMED, PROCESSING, SHIPPED, DELIVERED, COMPLETED.
 * PENDING/CANCELLED/REFUNDED are excluded from revenue calculations.
 *
 * <p>Date handling: {@code dateFrom} and {@code dateTo} are UTC {@link Instant} values.
 * Null means "no bound" (all-time). Passed directly to native queries.
 *
 * <p>Currency assumption: This system defaults to VND. All monetary fields are
 * returned with the currency code "VND". If multi-currency support is added
 * in a future sprint, revenue grouping by currency should be implemented here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final String DEFAULT_CURRENCY = "VND";
    private static final int    RATE_SCALE       = 2;
    private static final BigDecimal HUNDRED       = new BigDecimal("100");

    private final AnalyticsRepository analyticsRepository;

    // ── Overview ──────────────────────────────────────────────────────────────

    /**
     * Builds the high-level KPI overview for a time window.
     */
    public OverviewResponse getOverview(Instant dateFrom, Instant dateTo) {
        log.debug("getOverview: dateFrom={}, dateTo={}", dateFrom, dateTo);

        BigDecimal totalRevenue    = nullSafe(analyticsRepository.sumRevenue(dateFrom, dateTo));
        long       totalOrders     = analyticsRepository.countAllOrders(dateFrom, dateTo);
        long       totalCustomers  = analyticsRepository.countDistinctCustomers(dateFrom, dateTo);
        long       productsSold    = analyticsRepository.sumProductsSold(dateFrom, dateTo);
        long       cancelledOrders = analyticsRepository.countCancelledOrders(dateFrom, dateTo);

        // Count revenue-eligible orders (used as denominator for AOV)
        long revenueEligibleOrders = countRevenueEligibleOrders(dateFrom, dateTo);

        BigDecimal averageOrderValue = revenueEligibleOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(revenueEligibleOrders), RATE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal cancellationRate = totalOrders > 0
                ? HUNDRED.multiply(BigDecimal.valueOf(cancelledOrders))
                         .divide(BigDecimal.valueOf(totalOrders), RATE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new OverviewResponse(
                totalRevenue,
                totalOrders,
                totalCustomers,
                productsSold,
                averageOrderValue,
                cancelledOrders,
                cancellationRate,
                DEFAULT_CURRENCY,
                dateFrom,
                dateTo
        );
    }

    // ── Revenue time series ───────────────────────────────────────────────────

    /**
     * Returns revenue as a time series grouped by DAY, WEEK, or MONTH.
     *
     * @param dateFrom  start of window (null = all-time)
     * @param dateTo    end of window (null = now)
     * @param groupBy   granularity string: DAY, WEEK, or MONTH (default: DAY)
     */
    public RevenueResponse getRevenue(Instant dateFrom, Instant dateTo, String groupBy) {
        log.debug("getRevenue: dateFrom={}, dateTo={}, groupBy={}", dateFrom, dateTo, groupBy);

        String normalizedGroupBy = groupBy != null ? groupBy.toUpperCase() : "DAY";

        List<RevenueTimeSeriesRow> rows = switch (normalizedGroupBy) {
            case "WEEK"  -> analyticsRepository.revenueByWeek(dateFrom, dateTo);
            case "MONTH" -> analyticsRepository.revenueByMonth(dateFrom, dateTo);
            default      -> analyticsRepository.revenueByDay(dateFrom, dateTo);
        };

        List<RevenuePeriodResponse> data = rows.stream()
                .map(r -> new RevenuePeriodResponse(
                        r.getPeriod(),
                        nullSafe(r.getRevenue()),
                        r.getOrders()))
                .toList();

        return new RevenueResponse(normalizedGroupBy, DEFAULT_CURRENCY, dateFrom, dateTo, data);
    }

    // ── Order analytics ───────────────────────────────────────────────────────

    /**
     * Returns per-status order counts and derived completion/cancellation rates.
     */
    public OrderAnalyticsResponse getOrderAnalytics(Instant dateFrom, Instant dateTo) {
        log.debug("getOrderAnalytics: dateFrom={}, dateTo={}", dateFrom, dateTo);

        List<StatusCountRow> rows = analyticsRepository.countByStatus(dateFrom, dateTo);

        // Build a map from status name → count for O(1) access
        Map<String, Long> countByStatus = new LinkedHashMap<>();
        for (StatusCountRow row : rows) {
            countByStatus.put(row.getStatus(), row.getOrderCount());
        }

        long pending    = countByStatus.getOrDefault("PENDING",    0L);
        long confirmed  = countByStatus.getOrDefault("CONFIRMED",  0L);
        long processing = countByStatus.getOrDefault("PROCESSING", 0L);
        long shipped    = countByStatus.getOrDefault("SHIPPED",    0L);
        long delivered  = countByStatus.getOrDefault("DELIVERED",  0L);
        long completed  = countByStatus.getOrDefault("COMPLETED",  0L);
        long cancelled  = countByStatus.getOrDefault("CANCELLED",  0L);

        long totalOrders = pending + confirmed + processing + shipped + delivered + completed + cancelled
                + countByStatus.getOrDefault("REFUNDED", 0L);

        BigDecimal completionRate = totalOrders > 0
                ? HUNDRED.multiply(BigDecimal.valueOf(completed))
                         .divide(BigDecimal.valueOf(totalOrders), RATE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal cancellationRate = totalOrders > 0
                ? HUNDRED.multiply(BigDecimal.valueOf(cancelled))
                         .divide(BigDecimal.valueOf(totalOrders), RATE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new OrderAnalyticsResponse(
                totalOrders, pending, confirmed, processing, shipped, delivered, completed, cancelled,
                completionRate, cancellationRate,
                dateFrom, dateTo
        );
    }

    // ── Top products ──────────────────────────────────────────────────────────

    /**
     * Returns the top N products by revenue, using historical snapshot fields.
     *
     * @param dateFrom  start of window (null = all-time)
     * @param dateTo    end of window (null = now)
     * @param limit     max entries to return (1–50, clamped)
     */
    public List<TopProductEntry> getTopProducts(Instant dateFrom, Instant dateTo, int limit) {
        log.debug("getTopProducts: dateFrom={}, dateTo={}, limit={}", dateFrom, dateTo, limit);

        int clampedLimit = Math.max(1, Math.min(50, limit));
        List<TopProductRow> rows = analyticsRepository.topProductsByRevenue(dateFrom, dateTo, clampedLimit);

        return rows.stream()
                .map(r -> new TopProductEntry(
                        r.getProductId(),
                        r.getSku(),
                        r.getProductName(),
                        r.getQuantitySold(),
                        nullSafe(r.getRevenue())))
                .toList();
    }

    // ── Customer analytics ────────────────────────────────────────────────────

    /**
     * Returns customer engagement metrics.
     *
     * <p>New customers = customers whose total order count across ALL time equals
     * their orders-in-period count (meaning all their orders are in this period).
     * Repeat customers = those with &gt;1 order in the period.
     */
    public CustomerAnalyticsResponse getCustomerAnalytics(Instant dateFrom, Instant dateTo) {
        log.debug("getCustomerAnalytics: dateFrom={}, dateTo={}", dateFrom, dateTo);

        List<CustomerOrderCountRow> rows = analyticsRepository.customerOrderCounts(dateFrom, dateTo);

        long uniqueCustomers  = rows.size();
        long newCustomers     = rows.stream()
                .filter(r -> r.getTotalOrdersAllTime() == r.getOrdersInPeriod())
                .count();
        long repeatCustomers  = rows.stream()
                .filter(r -> r.getOrdersInPeriod() > 1)
                .count();

        long totalOrdersInPeriod = rows.stream()
                .mapToLong(CustomerOrderCountRow::getOrdersInPeriod)
                .sum();

        BigDecimal averageOrdersPerCustomer = uniqueCustomers > 0
                ? BigDecimal.valueOf(totalOrdersInPeriod)
                            .divide(BigDecimal.valueOf(uniqueCustomers), RATE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new CustomerAnalyticsResponse(
                uniqueCustomers, newCustomers, repeatCustomers,
                averageOrdersPerCustomer,
                dateFrom, dateTo
        );
    }

    // ── Payment analytics ─────────────────────────────────────────────────────

    /**
     * Returns payment totals grouped by payment method.
     * Methods with no activity in the period are omitted from the map.
     */
    public PaymentAnalyticsResponse getPaymentAnalytics(Instant dateFrom, Instant dateTo) {
        log.debug("getPaymentAnalytics: dateFrom={}, dateTo={}", dateFrom, dateTo);

        List<PaymentMethodRow> rows = analyticsRepository.paymentBreakdown(dateFrom, dateTo);

        Map<String, PaymentMethodStats> breakdown = new LinkedHashMap<>();
        for (PaymentMethodRow row : rows) {
            breakdown.put(
                    row.getMethod(),
                    new PaymentMethodStats(row.getOrders(), nullSafe(row.getAmount()))
            );
        }

        return new PaymentAnalyticsResponse(DEFAULT_CURRENCY, breakdown, dateFrom, dateTo);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Counts revenue-eligible orders (CONFIRMED through COMPLETED) in the period.
     * Used as denominator for average order value.
     */
    private long countRevenueEligibleOrders(Instant dateFrom, Instant dateTo) {
        // Derive from the status-count query — sum eligible statuses only
        List<StatusCountRow> rows = analyticsRepository.countByStatus(dateFrom, dateTo);
        return rows.stream()
                .filter(r -> {
                    String s = r.getStatus();
                    return "CONFIRMED".equals(s) || "PROCESSING".equals(s) || "SHIPPED".equals(s)
                            || "DELIVERED".equals(s) || "COMPLETED".equals(s);
                })
                .mapToLong(StatusCountRow::getOrderCount)
                .sum();
    }

    /** Converts a null BigDecimal from a native query to BigDecimal.ZERO. */
    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
