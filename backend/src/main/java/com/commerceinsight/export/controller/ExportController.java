package com.commerceinsight.export.controller;

import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.export.dto.ExportFile;
import com.commerceinsight.export.service.ExportService;
import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ExportController — read-only report downloads (Sprint 11A).
 *
 * <p>Base path: {@code /api/v1/export}. Every endpoint accepts {@code format=xlsx}
 * or {@code format=pdf} (case-insensitive) and streams a binary file back with a
 * {@code Content-Disposition: attachment} header — the {@code ApiResponse}
 * envelope is intentionally not used for binary payloads.
 *
 * <p>Security: {@code STAFF}, {@code MANAGER} or {@code ADMIN} — matching the
 * read endpoints of the Import module. Thin HTTP adapter: all work happens in
 * {@link ExportService}.
 */
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
@Tag(name = "Export", description = "Read-only XLSX / PDF report exports")
public class ExportController {

    private final ExportService exportService;

    // ── Products ────────────────────────────────────────────────────────────

    @GetMapping("/products")
    @Operation(summary = "Export the product catalogue as XLSX or PDF")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(defaultValue = "xlsx") String format,
            @Parameter(description = "Search on name or SKU") @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax) {

        return download(exportService.exportProducts(format, search, categoryId, active, priceMin, priceMax));
    }

    // ── Customers ───────────────────────────────────────────────────────────

    @GetMapping("/customers")
    @Operation(summary = "Export customers as XLSX or PDF")
    public ResponseEntity<byte[]> exportCustomers(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        return download(exportService.exportCustomers(format, keyword, status, groupId, startDate, endDate));
    }

    // ── Orders ──────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    @Operation(summary = "Export orders (one row per order) as XLSX or PDF")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        return download(exportService.exportOrders(
                format, keyword, customerId, status, paymentStatus, dateFrom, dateTo));
    }

    // ── Analytics ───────────────────────────────────────────────────────────

    @GetMapping("/analytics/revenue")
    @Operation(summary = "Export the revenue time series as XLSX or PDF")
    public ResponseEntity<byte[]> exportRevenueAnalytics(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(defaultValue = "DAY") String groupBy) {

        return download(exportService.exportRevenueAnalytics(format, dateFrom, dateTo, groupBy));
    }

    @GetMapping("/analytics/orders")
    @Operation(summary = "Export order analytics (status breakdown + rates) as XLSX or PDF")
    public ResponseEntity<byte[]> exportOrderAnalytics(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        return download(exportService.exportOrderAnalytics(format, dateFrom, dateTo));
    }

    @GetMapping("/analytics/products")
    @Operation(summary = "Export the top-products-by-revenue leaderboard as XLSX or PDF")
    public ResponseEntity<byte[]> exportTopProductsAnalytics(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(defaultValue = "10") int limit) {

        return download(exportService.exportTopProductsAnalytics(format, dateFrom, dateTo, limit));
    }

    @GetMapping("/analytics/customers")
    @Operation(summary = "Export customer engagement analytics as XLSX or PDF")
    public ResponseEntity<byte[]> exportCustomerAnalytics(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        return download(exportService.exportCustomerAnalytics(format, dateFrom, dateTo));
    }

    @GetMapping("/analytics/payments")
    @Operation(summary = "Export payment-method analytics as XLSX or PDF")
    public ResponseEntity<byte[]> exportPaymentAnalytics(
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {

        return download(exportService.exportPaymentAnalytics(format, dateFrom, dateTo));
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private static ResponseEntity<byte[]> download(ExportFile file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.size())
                .body(file.content());
    }
}
