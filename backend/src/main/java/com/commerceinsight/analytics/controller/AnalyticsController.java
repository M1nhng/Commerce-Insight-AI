package com.commerceinsight.analytics.controller;

import com.commerceinsight.analytics.dto.*;
import com.commerceinsight.analytics.service.AnalyticsService;
import com.commerceinsight.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * AnalyticsController — read-only analytics endpoints.
 *
 * <p>Base path: {@code /api/v1/analytics}
 *
 * <p>Security: all endpoints require authentication.
 * Any authenticated role (STAFF, MANAGER, ADMIN) may access analytics.
 * Method-level @PreAuthorize("isAuthenticated()") used — consistent with
 * the existing SecurityConfig which already requires authentication by default.
 *
 * <p>Architecture rule: thin HTTP adapter. No business logic here.
 * All computation delegated to {@link AnalyticsService}.
 *
 * <p>Date parameters use ISO 8601 Instant format consistent with
 * {@code OrderController} — e.g. {@code 2026-08-01T00:00:00Z}.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Analytics", description = "Read-only ecommerce analytics and KPI aggregations")
@SecurityRequirement(name = "Bearer Authentication")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ── GET /api/v1/analytics/overview ────────────────────────────────────────

    @GetMapping("/overview")
    @Operation(
            summary = "Dashboard KPI overview",
            description = """
                    Returns high-level ecommerce KPIs for a given time window:
                    total revenue, order count, unique customers, products sold,
                    average order value, cancelled orders, and cancellation rate.
                    Revenue figures exclude PENDING, CANCELLED, and REFUNDED orders.
                    All monetary values are in the system currency (VND).
                    """
    )
    public ResponseEntity<ApiResponse<OverviewResponse>> getOverview(

            @Parameter(description = "Start of period (ISO 8601, e.g. 2026-08-01T00:00:00Z). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "End of period (ISO 8601, e.g. 2026-08-31T23:59:59Z). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getOverview(dateFrom, dateTo),
                "Overview retrieved successfully"
        ));
    }

    // ── GET /api/v1/analytics/revenue ─────────────────────────────────────────

    @GetMapping("/revenue")
    @Operation(
            summary = "Revenue time series",
            description = """
                    Returns revenue as a time series, grouped by DAY, WEEK, or MONTH.
                    Each data point contains the period label, total revenue, and order count.
                    Only revenue-eligible orders (CONFIRMED → COMPLETED) are included.
                    """
    )
    public ResponseEntity<ApiResponse<RevenueResponse>> getRevenue(

            @Parameter(description = "Start of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "End of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo,

            @Parameter(description = "Time grouping granularity: DAY (default), WEEK, or MONTH")
            @RequestParam(required = false, defaultValue = "DAY")
            String groupBy
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getRevenue(dateFrom, dateTo, groupBy),
                "Revenue data retrieved successfully"
        ));
    }

    // ── GET /api/v1/analytics/orders ──────────────────────────────────────────

    @GetMapping("/orders")
    @Operation(
            summary = "Order analytics by status",
            description = """
                    Returns order counts broken down by every order status,
                    plus the overall completion rate and cancellation rate as percentages.
                    Rates are 0.00 when there are no orders in the period.
                    """
    )
    public ResponseEntity<ApiResponse<OrderAnalyticsResponse>> getOrderAnalytics(

            @Parameter(description = "Start of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "End of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getOrderAnalytics(dateFrom, dateTo),
                "Order analytics retrieved successfully"
        ));
    }

    // ── GET /api/v1/analytics/products/top ───────────────────────────────────

    @GetMapping("/products/top")
    @Operation(
            summary = "Top products by revenue",
            description = """
                    Returns the top N products ranked by total revenue generated
                    from revenue-eligible orders in the period.
                    Uses historical SKU and product name snapshots from order items —
                    results are accurate even if the product was later modified or deleted.
                    """
    )
    public ResponseEntity<ApiResponse<List<TopProductEntry>>> getTopProducts(

            @Parameter(description = "Start of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "End of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo,

            @Parameter(description = "Maximum number of products to return (1–50, default 10)")
            @RequestParam(required = false, defaultValue = "10")
            int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getTopProducts(dateFrom, dateTo, limit),
                "Top products retrieved successfully"
        ));
    }

    // ── GET /api/v1/analytics/customers ──────────────────────────────────────

    @GetMapping("/customers")
    @Operation(
            summary = "Customer engagement analytics",
            description = """
                    Returns customer engagement metrics for the period:
                    unique customers who ordered, new vs. returning customers,
                    and average orders per customer.
                    'New' customers are those placing their very first order in this period.
                    """
    )
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> getCustomerAnalytics(

            @Parameter(description = "Start of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "End of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getCustomerAnalytics(dateFrom, dateTo),
                "Customer analytics retrieved successfully"
        ));
    }

    // ── GET /api/v1/analytics/payments ───────────────────────────────────────

    @GetMapping("/payments")
    @Operation(
            summary = "Payment breakdown by method",
            description = """
                    Returns order counts and total payment amounts grouped by payment method
                    (CASH, BANK_TRANSFER, CARD, OTHER).
                    Methods with no activity in the period are omitted from the response.
                    """
    )
    public ResponseEntity<ApiResponse<PaymentAnalyticsResponse>> getPaymentAnalytics(

            @Parameter(description = "Start of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "End of period (ISO 8601). Omit for all-time.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getPaymentAnalytics(dateFrom, dateTo),
                "Payment analytics retrieved successfully"
        ));
    }
}
