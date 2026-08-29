package com.commerceinsight.export.service;

import com.commerceinsight.export.TestReports;
import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ColumnType;
import com.commerceinsight.export.dto.ExportFile;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.excel.ExcelExportWriter;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.export.pdf.PdfExportWriter;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExportService (facade)")
class ExportServiceTest {

    @Mock private ProductExportService productExportService;
    @Mock private CustomerExportService customerExportService;
    @Mock private OrderExportService orderExportService;
    @Mock private AnalyticsExportService analyticsExportService;

    private ExportService exportService;
    private ExportProperties properties;

    private static final Instant FROM = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-08-31T00:00:00Z");
    private static final String TODAY = LocalDate.now(ZoneOffset.UTC).toString();

    @BeforeEach
    void setUp() {
        properties = new ExportProperties();
        properties.setMaxRows(10_000);
        exportService = new ExportService(
                productExportService, customerExportService, orderExportService, analyticsExportService,
                new ExcelExportWriter(), new PdfExportWriter(), properties);

        lenient().when(productExportService.buildReport(any(), any(), any(), any(), any()))
                .thenReturn(TestReports.sample());
        lenient().when(customerExportService.buildReport(any(), any(), any(), any(), any()))
                .thenReturn(TestReports.sample());
        lenient().when(orderExportService.buildReport(any(), any(), any(), any(), any(), any()))
                .thenReturn(TestReports.sample());
        lenient().when(analyticsExportService.buildRevenueReport(any(), any(), any()))
                .thenReturn(TestReports.sample());
        lenient().when(analyticsExportService.buildOrderAnalyticsReport(any(), any()))
                .thenReturn(TestReports.sample());
        lenient().when(analyticsExportService.buildTopProductsReport(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(TestReports.sample());
        lenient().when(analyticsExportService.buildCustomerAnalyticsReport(any(), any()))
                .thenReturn(TestReports.sample());
        lenient().when(analyticsExportService.buildPaymentAnalyticsReport(any(), any()))
                .thenReturn(TestReports.sample());
    }

    // ── Format handling ─────────────────────────────────────────────────────

    @Test
    @DisplayName("an unsupported format is rejected 400 and the report is never built")
    void invalidFormatRejected() {
        assertThatThrownBy(() -> exportService.exportProducts("csv", null, null, null, null, null))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> assertThat(((ExportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXPORT_INVALID_FORMAT));
        verifyNoInteractions(productExportService);
    }

    @Test
    @DisplayName("XLSX request → .xlsx filename, spreadsheet content type, real ZIP/OOXML bytes")
    void xlsxPipeline() throws Exception {
        ExportFile file = exportService.exportProducts("xlsx", null, null, null, null, null);

        assertThat(file.filename()).isEqualTo("products_" + TODAY + ".xlsx");
        assertThat(file.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(file.content()[0]).isEqualTo((byte) 'P');
        assertThat(file.content()[1]).isEqualTo((byte) 'K');
        try (var wb = TestReports.openXlsx(file.content())) {
            assertThat(wb.getNumberOfSheets()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    @DisplayName("PDF request → .pdf filename, application/pdf, valid %PDF header (case-insensitive format)")
    void pdfPipeline() {
        ExportFile file = exportService.exportProducts("PDF", null, null, null, null, null);

        assertThat(file.filename()).isEqualTo("products_" + TODAY + ".pdf");
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(new String(file.content(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    // ── Date-range validation ───────────────────────────────────────────────

    @Test
    @DisplayName("dateFrom after dateTo → 400 EXPORT_INVALID_DATE_RANGE, downstream never called (orders)")
    void invalidDateRangeOrders() {
        assertThatThrownBy(() -> exportService.exportOrders("xlsx", null, null, null, null, TO, FROM))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> {
                    ExportException e = (ExportException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXPORT_INVALID_DATE_RANGE);
                    assertThat(e.getMessage()).isEqualTo("dateFrom must not be after dateTo");
                });
        verifyNoInteractions(orderExportService);
    }

    @Test
    @DisplayName("dateFrom after dateTo → analytics revenue is short-circuited too")
    void invalidDateRangeAnalytics() {
        assertThatThrownBy(() -> exportService.exportRevenueAnalytics("xlsx", TO, FROM, "DAY"))
                .isInstanceOf(ExportException.class);
        verifyNoInteractions(analyticsExportService);
    }

    @Test
    @DisplayName("equal dateFrom / dateTo is a valid range")
    void equalDatesAccepted() {
        ExportFile file = exportService.exportOrders("xlsx", null, null, null, null, FROM, FROM);
        assertThat(file.size()).isPositive();
    }

    @Test
    @DisplayName("no date bounds is a valid range")
    void nullDatesAccepted() {
        assertThat(exportService.exportCustomerAnalytics("xlsx", null, null).size()).isPositive();
    }

    // ── Row-limit backstop ──────────────────────────────────────────────────

    @Test
    @DisplayName("a report whose total rows exceed app.export.max-rows is rejected 422")
    void rowLimitBackstop() {
        properties.setMaxRows(1);
        ReportDocument big = ReportDocument.single("Big", new ReportTable(
                "Rows",
                List.of(new ReportColumn("A", ColumnType.TEXT)),
                List.of(TestReports.newRow("r1"), TestReports.newRow("r2"))));
        when(customerExportService.buildReport(any(), any(), any(), any(), any())).thenReturn(big);

        assertThatThrownBy(() -> exportService.exportCustomers("xlsx", null, null, null, null, null))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> assertThat(((ExportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXPORT_ROW_LIMIT_EXCEEDED));
    }

    // ── Every endpoint produces a file in both formats ──────────────────────

    @Test
    @DisplayName("all eight export operations yield a non-empty file with the right extension")
    void allOperationsProduceFiles() {
        assertThat(exportService.exportProducts("xlsx", null, null, null, null, null).filename()).endsWith(".xlsx");
        assertThat(exportService.exportProducts("pdf", null, null, null, null, null).filename()).endsWith(".pdf");
        assertThat(exportService.exportCustomers("xlsx", null, null, null, null, null).size()).isPositive();
        assertThat(exportService.exportCustomers("pdf", null, null, null, null, null).size()).isPositive();
        assertThat(exportService.exportOrders("xlsx", null, null, null, null, null, null).size()).isPositive();
        assertThat(exportService.exportOrders("pdf", null, null, null, null, null, null).size()).isPositive();
        assertThat(exportService.exportRevenueAnalytics("xlsx", null, null, "DAY").filename())
                .isEqualTo("revenue_analytics_" + TODAY + ".xlsx");
        assertThat(exportService.exportRevenueAnalytics("pdf", null, null, "DAY").size()).isPositive();
        assertThat(exportService.exportOrderAnalytics("xlsx", null, null).filename())
                .isEqualTo("order_analytics_" + TODAY + ".xlsx");
        assertThat(exportService.exportOrderAnalytics("pdf", null, null).size()).isPositive();
        assertThat(exportService.exportTopProductsAnalytics("xlsx", null, null, 10).filename())
                .isEqualTo("top_products_analytics_" + TODAY + ".xlsx");
        assertThat(exportService.exportTopProductsAnalytics("pdf", null, null, 10).size()).isPositive();
        assertThat(exportService.exportCustomerAnalytics("xlsx", null, null).size()).isPositive();
        assertThat(exportService.exportCustomerAnalytics("pdf", null, null).size()).isPositive();
        assertThat(exportService.exportPaymentAnalytics("xlsx", null, null).filename())
                .isEqualTo("payment_analytics_" + TODAY + ".xlsx");
        assertThat(exportService.exportPaymentAnalytics("pdf", null, null).size()).isPositive();
    }
}
