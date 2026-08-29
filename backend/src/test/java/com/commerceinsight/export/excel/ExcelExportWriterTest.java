package com.commerceinsight.export.excel;

import com.commerceinsight.export.TestReports;
import com.commerceinsight.export.dto.ColumnType;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.shared.exception.ErrorCode;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExcelExportWriter")
class ExcelExportWriterTest {

    private final ExcelExportWriter writer = new ExcelExportWriter();

    @Test
    @DisplayName("produces a workbook POI can reopen, with a titled sheet")
    void producesValidWorkbook() throws Exception {
        byte[] bytes = writer.write(TestReports.sample());

        try (Workbook wb = TestReports.openXlsx(bytes)) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Test");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Sample Export");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).startsWith("Generated:");
        }
    }

    @Test
    @DisplayName("writes a bold, frozen header row at the expected position")
    void headerRowIsBoldAndFrozen() throws Exception {
        byte[] bytes = writer.write(TestReports.sample());

        try (Workbook wb = TestReports.openXlsx(bytes)) {
            Sheet sheet = wb.getSheetAt(0);
            var header = sheet.getRow(3);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("When (UTC)");

            int fontIdx = header.getCell(0).getCellStyle().getFontIndex();
            assertThat(wb.getFontAt(fontIdx).getBold()).isTrue();

            assertThat(sheet.getPaneInformation()).isNotNull();
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) 4);
        }
    }

    @Test
    @DisplayName("writes typed cells: text, numeric money/percent/integer, real datetime, and blanks for null")
    void writesTypedCells() throws Exception {
        byte[] bytes = writer.write(TestReports.sample());

        try (Workbook wb = TestReports.openXlsx(bytes)) {
            Sheet sheet = wb.getSheetAt(0);
            var first = sheet.getRow(4);
            assertThat(first.getCell(0).getStringCellValue()).isEqualTo("Alpha widget");
            assertThat(first.getCell(1).getNumericCellValue()).isEqualTo(199.99);
            assertThat(first.getCell(2).getNumericCellValue()).isEqualTo(3.0);
            assertThat(first.getCell(3).getNumericCellValue()).isEqualTo(42.50);
            assertThat(first.getCell(4).getLocalDateTimeCellValue())
                    .isEqualTo(LocalDateTime.ofInstant(TestReports.TS, ZoneOffset.UTC));

            var second = sheet.getRow(5);
            assertThat(second.getCell(0).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    @DisplayName("renders one worksheet per table")
    void multipleTablesBecomeMultipleSheets() throws Exception {
        byte[] bytes = writer.write(TestReports.twoTables());

        try (Workbook wb = TestReports.openXlsx(bytes)) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("First");
            assertThat(wb.getSheetName(1)).isEqualTo("Second");
        }
    }

    @Test
    @DisplayName("an empty dataset still produces a valid header-only sheet")
    void emptyDatasetIsValid() throws Exception {
        ReportDocument doc = ReportDocument.single("Empty", new ReportTable(
                "Rows", List.of(new ReportColumn("A", ColumnType.TEXT)), List.of()));

        byte[] bytes = writer.write(doc);
        try (Workbook wb = TestReports.openXlsx(bytes)) {
            assertThat(wb.getSheetAt(0).getRow(3).getCell(0).getStringCellValue()).isEqualTo("A");
        }
    }

    @Test
    @DisplayName("wraps an internal rendering failure as EXPORT_FAILED (500), never leaks the cause")
    void wrapsGenerationFailure() {
        ReportDocument bad = ReportDocument.single("Bad", new ReportTable(
                "Rows",
                List.of(new ReportColumn("When", ColumnType.DATETIME)),
                List.of(TestReports.newRow("not-an-instant"))));

        assertThatThrownBy(() -> writer.write(bad))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> assertThat(((ExportException) ex).getErrorCode()).isEqualTo(ErrorCode.EXPORT_FAILED))
                .hasMessage("Unable to generate the export. Please try again.");
    }
}
