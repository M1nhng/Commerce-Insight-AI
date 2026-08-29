package com.commerceinsight.export.service;

import com.commerceinsight.analytics.dto.CustomerAnalyticsResponse;
import com.commerceinsight.analytics.dto.OrderAnalyticsResponse;
import com.commerceinsight.analytics.dto.PaymentAnalyticsResponse;
import com.commerceinsight.analytics.dto.PaymentMethodStats;
import com.commerceinsight.analytics.dto.RevenuePeriodResponse;
import com.commerceinsight.analytics.dto.RevenueResponse;
import com.commerceinsight.analytics.dto.TopProductEntry;
import com.commerceinsight.analytics.service.AnalyticsService;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * AnalyticsExportService — turns the existing analytics responses into
 * format-neutral {@link ReportDocument}s.
 *
 * <p>{@link AnalyticsService} remains the single source of truth for every
 * figure: this class performs <b>no</b> aggregation, ranking, rate calculation
 * or currency maths — it only reshapes the DTOs into rows.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsExportService {

    private final AnalyticsService analyticsService;

    // ── Revenue ─────────────────────────────────────────────────────────────

    public ReportDocument buildRevenueReport(Instant dateFrom, Instant dateTo, String groupBy) {
        RevenueResponse revenue = analyticsService.getRevenue(dateFrom, dateTo, groupBy);

        List<ReportColumn> columns = List.of(
                ReportColumn.text("Period"),
                ReportColumn.money("Revenue"),
                ReportColumn.integer("Orders"),
                ReportColumn.text("Currency"));

        List<List<Object>> rows = new ArrayList<>();
        for (RevenuePeriodResponse point : revenue.data()) {
            rows.add(row(point.period(), point.revenue(), point.orders(), revenue.currency()));
        }

        String title = "Revenue Analytics Export (groupBy=" + revenue.groupBy() + ")";
        return ReportDocument.single(title, Instant.now(), new ReportTable("Revenue", columns, rows));
    }

    // ── Order analytics ─────────────────────────────────────────────────────

    public ReportDocument buildOrderAnalyticsReport(Instant dateFrom, Instant dateTo) {
        OrderAnalyticsResponse a = analyticsService.getOrderAnalytics(dateFrom, dateTo);

        List<ReportColumn> columns = List.of(
                ReportColumn.integer("Total Orders"),
                ReportColumn.integer("Pending Orders"),
                ReportColumn.integer("Confirmed Orders"),
                ReportColumn.integer("Processing Orders"),
                ReportColumn.integer("Shipped Orders"),
                ReportColumn.integer("Delivered Orders"),
                ReportColumn.integer("Completed Orders"),
                ReportColumn.integer("Cancelled Orders"),
                ReportColumn.percent("Completion Rate (%)"),
                ReportColumn.percent("Cancellation Rate (%)"));

        List<Object> row = row(
                a.totalOrders(), a.pendingOrders(), a.confirmedOrders(), a.processingOrders(),
                a.shippedOrders(), a.deliveredOrders(), a.completedOrders(), a.cancelledOrders(),
                a.completionRate(), a.cancellationRate());

        return ReportDocument.single("Order Analytics Export", Instant.now(),
                new ReportTable("Order Analytics", columns, List.of(row)));
    }

    // ── Top products ────────────────────────────────────────────────────────

    public ReportDocument buildTopProductsReport(Instant dateFrom, Instant dateTo, int limit) {
        List<TopProductEntry> entries = analyticsService.getTopProducts(dateFrom, dateTo, limit);

        List<ReportColumn> columns = List.of(
                ReportColumn.text("Product ID"),
                ReportColumn.text("SKU"),
                ReportColumn.text("Product Name"),
                ReportColumn.integer("Quantity Sold"),
                ReportColumn.money("Revenue"));

        List<List<Object>> rows = new ArrayList<>();
        for (TopProductEntry e : entries) {
            rows.add(row(
                    e.productId() != null ? e.productId().toString() : "",
                    e.sku(),
                    e.productName(),
                    e.quantitySold(),
                    e.revenue()));
        }

        return ReportDocument.single("Top Products Analytics Export", Instant.now(),
                new ReportTable("Top Products", columns, rows));
    }

    // ── Customer analytics ──────────────────────────────────────────────────

    public ReportDocument buildCustomerAnalyticsReport(Instant dateFrom, Instant dateTo) {
        CustomerAnalyticsResponse a = analyticsService.getCustomerAnalytics(dateFrom, dateTo);

        List<ReportColumn> columns = List.of(
                ReportColumn.integer("Unique Customers"),
                ReportColumn.integer("New Customers"),
                ReportColumn.integer("Repeat Customers"),
                ReportColumn.decimal("Avg Orders / Customer"));

        List<Object> row = row(
                a.uniqueCustomers(), a.newCustomers(), a.repeatCustomers(), a.averageOrdersPerCustomer());

        return ReportDocument.single("Customer Analytics Export", Instant.now(),
                new ReportTable("Customer Analytics", columns, List.of(row)));
    }

    // ── Payment analytics ───────────────────────────────────────────────────

    public ReportDocument buildPaymentAnalyticsReport(Instant dateFrom, Instant dateTo) {
        PaymentAnalyticsResponse a = analyticsService.getPaymentAnalytics(dateFrom, dateTo);

        List<ReportColumn> columns = List.of(
                ReportColumn.text("Payment Method"),
                ReportColumn.integer("Orders"),
                ReportColumn.money("Amount"),
                ReportColumn.text("Currency"));

        // TreeMap only to give the rows a deterministic order — values untouched.
        Map<String, PaymentMethodStats> breakdown = a.breakdown() == null
                ? Map.of() : new TreeMap<>(a.breakdown());

        List<List<Object>> rows = new ArrayList<>();
        for (Map.Entry<String, PaymentMethodStats> entry : breakdown.entrySet()) {
            PaymentMethodStats stats = entry.getValue();
            rows.add(row(entry.getKey(), stats.orders(), stats.amount(), a.currency()));
        }

        return ReportDocument.single("Payment Analytics Export", Instant.now(),
                new ReportTable("Payment Analytics", columns, rows));
    }

    /** Null-tolerant row builder ({@code List.of} rejects nulls). */
    private static List<Object> row(Object... values) {
        List<Object> out = new ArrayList<>(values.length);
        for (Object value : values) {
            out.add(value);
        }
        return out;
    }
}
