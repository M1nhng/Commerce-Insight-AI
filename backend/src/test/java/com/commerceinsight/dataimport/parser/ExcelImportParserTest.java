package com.commerceinsight.dataimport.parser;

import com.commerceinsight.exception.ImportException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ExcelImportParserTest — unit tests for {@link ExcelImportParser}.
 */
@DisplayName("ExcelImportParser Unit Tests")
class ExcelImportParserTest {

    private final ExcelImportParser parser = new ExcelImportParser();
    private static final String[] HEADERS = {"sku", "name", "price"};

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse valid XLSX — returns rows with correct values")
    void parseValidXlsx_returnsRows() throws IOException {
        InputStream is = buildXlsx(
                new String[]{"sku", "name", "price"},
                new String[][]{{"SKU-001", "Widget", "100.00"}, {"SKU-002", "Gadget", "200.50"}}
        );

        List<ParsedRow> rows = parser.parse(is, HEADERS, 100);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
        assertThat(rows.get(0).get("name")).isEqualTo("Widget");
        assertThat(rows.get(0).get("price")).isEqualTo("100.00");
        assertThat(rows.get(1).get("sku")).isEqualTo("SKU-002");
    }

    @Test
    @DisplayName("Parse XLSX with numeric cell — returns string without decimal for whole numbers")
    void parseNumericCell_wholeNumberFormatted() throws IOException {
        InputStream is = buildXlsxWithNumeric(
                new String[]{"sku", "name", "price"},
                new Object[][]{{"SKU-001", "Widget", 100.0}}
        );

        List<ParsedRow> rows = parser.parse(is, HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("price")).isEqualTo("100");
    }

    @Test
    @DisplayName("Parse XLSX with blank cell — returns empty string")
    void parseBlankCell_returnsEmpty() throws IOException {
        InputStream is = buildXlsx(
                new String[]{"sku", "name", "price", "description"},
                new String[][]{{"SKU-001", "Widget", "100.00", ""}}
        );

        List<ParsedRow> rows = parser.parse(is, HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("description")).isEmpty();
        assertThat(rows.get(0).hasValue("description")).isFalse();
    }

    @Test
    @DisplayName("Parse XLSX case-insensitive headers — values accessible by lowercase key")
    void parseCaseInsensitiveHeaders_accessible() throws IOException {
        InputStream is = buildXlsx(
                new String[]{"SKU", "NAME", "PRICE"},
                new String[][]{{"SKU-001", "Widget", "100.00"}}
        );

        List<ParsedRow> rows = parser.parse(is, HEADERS, 100);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Missing required header — throws ImportException with INVALID_HEADER")
    void missingRequiredHeader_throwsImportException() throws IOException {
        InputStream is = buildXlsx(
                new String[]{"name", "price"},
                new String[][]{{"Widget", "100.00"}}
        );

        assertThatThrownBy(() -> parser.parse(is, HEADERS, 100))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("INVALID_HEADER")
                .hasMessageContaining("sku");
    }

    @Test
    @DisplayName("Row limit exceeded — throws ImportException with ROW_LIMIT_EXCEEDED")
    void rowLimitExceeded_throwsImportException() throws IOException {
        InputStream is = buildXlsx(
                new String[]{"sku", "name", "price"},
                new String[][]{{"SKU-001", "W", "1"}, {"SKU-002", "W", "2"}, {"SKU-003", "W", "3"}}
        );

        assertThatThrownBy(() -> parser.parse(is, HEADERS, 2))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("ROW_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Empty rows are skipped — fully blank rows excluded from result")
    void emptyRowsSkipped() throws IOException {
        // Build a workbook with an empty row between data rows
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("sku");
        header.createCell(1).setCellValue("name");
        header.createCell(2).setCellValue("price");

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("SKU-001");
        row1.createCell(1).setCellValue("Widget");
        row1.createCell(2).setCellValue("100.00");

        // Row 2 is intentionally left empty (simulates blank row in Excel)

        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("SKU-002");
        row3.createCell(1).setCellValue("Gadget");
        row3.createCell(2).setCellValue("200.00");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();

        List<ParsedRow> rows = parser.parse(new ByteArrayInputStream(bos.toByteArray()), HEADERS, 100);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
        assertThat(rows.get(1).get("sku")).isEqualTo("SKU-002");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private InputStream buildXlsx(String[] headers, String[][] data) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data[r].length; c++) {
                row.createCell(c).setCellValue(data[r][c]);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }

    private InputStream buildXlsxWithNumeric(String[] headers, Object[][] data) throws IOException {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Sheet1");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        for (int r = 0; r < data.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < data[r].length; c++) {
                Object val = data[r][c];
                Cell cell = row.createCell(c);
                if (val instanceof Number) {
                    cell.setCellValue(((Number) val).doubleValue());
                } else {
                    cell.setCellValue(val.toString());
                }
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }
}
