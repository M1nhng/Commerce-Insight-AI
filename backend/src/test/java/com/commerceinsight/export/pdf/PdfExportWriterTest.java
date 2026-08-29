package com.commerceinsight.export.pdf;

import com.commerceinsight.export.TestReports;
import com.commerceinsight.export.dto.ColumnType;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.shared.exception.ErrorCode;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PdfExportWriter")
class PdfExportWriterTest {

    private final PdfExportWriter writer = new PdfExportWriter();

    @Test
    @DisplayName("emits a well-formed PDF (valid %PDF header, at least one page)")
    void emitsValidPdf() throws Exception {
        byte[] bytes = writer.write(TestReports.sample());

        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        PdfReader reader = new PdfReader(bytes);
        try {
            assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        } finally {
            reader.close();
        }
    }

    @Test
    @DisplayName("page text contains the title, the generated stamp, headers and data values")
    void containsReportContent() throws Exception {
        String text = TestReports.pdfText(writer.write(TestReports.sample()));

        assertThat(text).contains("Sample Export");
        assertThat(text).contains("Generated:");
        assertThat(text).contains("Name").contains("Amount").contains("When (UTC)");
        assertThat(text).contains("Alpha widget");
        assertThat(text).contains("199.99");
        assertThat(text).contains("Page 1");
    }

    @Test
    @DisplayName("uses landscape orientation for wide reports")
    void usesLandscape() throws Exception {
        PdfReader reader = new PdfReader(writer.write(TestReports.sample()));
        try {
            var size = reader.getPageSizeWithRotation(1);
            assertThat(size.getWidth()).isGreaterThan(size.getHeight());
        } finally {
            reader.close();
        }
    }

    @Test
    @DisplayName("renders one labelled block per table for multi-table documents")
    void multiTableSections() throws Exception {
        String text = TestReports.pdfText(writer.write(TestReports.twoTables()));
        assertThat(text).contains("Multi Export").contains("First").contains("Second");
    }

    @Test
    @DisplayName("wraps an internal rendering failure as EXPORT_FAILED (500)")
    void wrapsGenerationFailure() {
        ReportDocument bad = ReportDocument.single("Bad", new ReportTable(
                "Rows",
                List.of(new ReportColumn("When", ColumnType.DATETIME)),
                List.of(TestReports.newRow("not-an-instant"))));

        assertThatThrownBy(() -> writer.write(bad))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> assertThat(((ExportException) ex).getErrorCode()).isEqualTo(ErrorCode.EXPORT_FAILED));
    }
}
