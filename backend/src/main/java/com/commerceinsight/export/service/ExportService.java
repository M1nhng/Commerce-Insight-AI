package com.commerceinsight.export.service;

import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ExportFile;
import com.commerceinsight.export.dto.ExportFormat;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.excel.ExcelExportWriter;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.export.pdf.PdfExportWriter;
import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * ExportService — the single entry point the controller talks to.
 *
 * <p>Responsibilities: parse the {@code format} parameter, validate the date
 * range, delegate report building to the matching {@code *ExportService},
 * enforce the row cap, render via the Excel / PDF writer, and package the bytes
 * with a deterministic filename.
 *
 * <p>It holds no business logic and never touches a repository — all figures
 * come from the domain services via the {@code *ExportService} helpers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ProductExportService productExportService;
    private final CustomerExportService customerExportService;
    private final OrderExportService orderExportService;
    private final AnalyticsExportService analyticsExportService;
    private final ExcelExportWriter excelExportWriter;
    private final PdfExportWriter pdfExportWriter;
    private final ExportProperties exportProperties;

    // ── Domain exports ──────────────────────────────────────────────────────

    public ExportFile exportProducts(String format, String search, UUID categoryId, Boolean active,
                                     BigDecimal priceMin, BigDecimal priceMax) {
        return render("products", format,
                () -> productExportService.buildReport(search, categoryId, active, priceMin, priceMax));
    }

    public ExportFile exportCustomers(String format, String keyword, CustomerStatus status, UUID groupId,
                                      Instant startDate, Instant endDate) {
        validateRange(startDate, endDate);
        return render("customers", format,
                () -> customerExportService.buildReport(keyword, status, groupId, startDate, endDate));
    }

    public ExportFile exportOrders(String format, String keyword, UUID customerId, OrderStatus status,
                                   PaymentStatus paymentStatus, Instant dateFrom, Instant dateTo) {
        validateRange(dateFrom, dateTo);
        return render("orders", format,
                () -> orderExportService.buildReport(keyword, customerId, status, paymentStatus, dateFrom, dateTo));
    }

    // ── Analytics exports ───────────────────────────────────────────────────

    public ExportFile exportRevenueAnalytics(String format, Instant dateFrom, Instant dateTo, String groupBy) {
        validateRange(dateFrom, dateTo);
        return render("revenue_analytics", format,
                () -> analyticsExportService.buildRevenueReport(dateFrom, dateTo, groupBy));
    }

    public ExportFile exportOrderAnalytics(String format, Instant dateFrom, Instant dateTo) {
        validateRange(dateFrom, dateTo);
        return render("order_analytics", format,
                () -> analyticsExportService.buildOrderAnalyticsReport(dateFrom, dateTo));
    }

    public ExportFile exportTopProductsAnalytics(String format, Instant dateFrom, Instant dateTo, int limit) {
        validateRange(dateFrom, dateTo);
        return render("top_products_analytics", format,
                () -> analyticsExportService.buildTopProductsReport(dateFrom, dateTo, limit));
    }

    public ExportFile exportCustomerAnalytics(String format, Instant dateFrom, Instant dateTo) {
        validateRange(dateFrom, dateTo);
        return render("customer_analytics", format,
                () -> analyticsExportService.buildCustomerAnalyticsReport(dateFrom, dateTo));
    }

    public ExportFile exportPaymentAnalytics(String format, Instant dateFrom, Instant dateTo) {
        validateRange(dateFrom, dateTo);
        return render("payment_analytics", format,
                () -> analyticsExportService.buildPaymentAnalyticsReport(dateFrom, dateTo));
    }

    // ── Internals ───────────────────────────────────────────────────────────

    /**
     * Validate the format, build the report, enforce the row cap, render, and
     * package. {@code documentSupplier} is only invoked after the format check
     * passes; date-range validation (where applicable) has already run in the
     * public method, so downstream services are never called for a bad range.
     */
    private ExportFile render(String baseName, String format, Supplier<ReportDocument> documentSupplier) {
        ExportFormat exportFormat = ExportFormat.from(format);

        long start = System.nanoTime();
        ReportDocument document = documentSupplier.get();

        int totalRows = document.totalRows();
        if (totalRows > exportProperties.getMaxRows()) {
            throw ExportException.rowLimitExceeded(exportProperties.getMaxRows());
        }

        byte[] content = switch (exportFormat) {
            case XLSX -> excelExportWriter.write(document);
            case PDF -> pdfExportWriter.write(document);
        };

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Export generated: report={}, format={}, rows={}, bytes={}, durationMs={}",
                baseName, exportFormat, totalRows, content.length, elapsedMs);

        return new ExportFile(fileName(baseName, exportFormat), exportFormat.contentType(), content);
    }

    private static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ExportException.invalidDateRange();
        }
    }

    private static String fileName(String baseName, ExportFormat format) {
        return baseName + "_" + FILE_DATE.format(LocalDate.now(ZoneOffset.UTC)) + "." + format.fileExtension();
    }
}
