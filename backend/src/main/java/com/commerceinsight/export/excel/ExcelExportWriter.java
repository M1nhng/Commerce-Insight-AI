package com.commerceinsight.export.excel;

import com.commerceinsight.export.dto.ColumnType;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.exception.ExportException;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ExcelExportWriter — renders a format-neutral {@link ReportDocument} into an
 * XLSX workbook.
 *
 * <p>Reusable across every report type. Contains no business logic — it only
 * lays out titles, headers and typed cells.
 *
 * <p>Uses a streaming {@link SXSSFWorkbook} (100-row window) so a 10k-row export
 * never fully materialises in the spreadsheet model.
 *
 * <p>Per worksheet: a title row, a "generated at" row, a bold header row with a
 * frozen pane and an auto-filter, then typed data rows with sensible column
 * widths and number / date formatting.
 */
@Component
public class ExcelExportWriter {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final int HEADER_ROW = 3;

    public byte[] write(ReportDocument document) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            ExcelStyleHelper styles = new ExcelStyleHelper(workbook);

            List<ReportTable> tables = document.tables();
            for (int i = 0; i < tables.size(); i++) {
                ReportTable table = tables.get(i);
                String sheetName = safeSheetName(table.name(), i);
                writeSheet(workbook.createSheet(sheetName), styles, document, table);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw ExportException.generationFailed(ex);
        }
    }

    private void writeSheet(SXSSFSheet sheet, ExcelStyleHelper styles,
                            ReportDocument document, ReportTable table) {

        List<ReportColumn> columns = table.columns();
        int colCount = columns.size();

        // ── Title + generated-at banner ─────────────────────────────────────
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(document.title());
        titleCell.setCellStyle(styles.title());

        Row metaRow = sheet.createRow(1);
        Cell metaCell = metaRow.createCell(0);
        metaCell.setCellValue("Generated: " + TS.format(
                document.generatedAt() != null ? document.generatedAt() : Instant.now()));
        metaCell.setCellStyle(styles.meta());

        if (colCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, colCount - 1));
        }

        // ── Header row ──────────────────────────────────────────────────────
        Row headerRow = sheet.createRow(HEADER_ROW);
        for (int c = 0; c < colCount; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(columns.get(c).header());
            cell.setCellStyle(styles.header());
        }

        // ── Data rows ───────────────────────────────────────────────────────
        List<List<Object>> rows = table.rows();
        for (int r = 0; r < rows.size(); r++) {
            Row row = sheet.createRow(HEADER_ROW + 1 + r);
            List<Object> values = rows.get(r);
            for (int c = 0; c < colCount; c++) {
                Cell cell = row.createCell(c);
                Object value = c < values.size() ? values.get(c) : null;
                writeCell(cell, styles, columns.get(c).type(), value);
            }
        }

        // ── Layout: widths, freeze pane, auto-filter ────────────────────────
        for (int c = 0; c < colCount; c++) {
            sheet.setColumnWidth(c, widthFor(columns.get(c).type()));
        }
        sheet.createFreezePane(0, HEADER_ROW + 1);
        int lastDataRow = HEADER_ROW + Math.max(rows.size(), 1);
        sheet.setAutoFilter(new CellRangeAddress(HEADER_ROW, lastDataRow, 0, Math.max(colCount - 1, 0)));
    }

    private void writeCell(Cell cell, ExcelStyleHelper styles, ColumnType type, Object value) {
        cell.setCellStyle(styles.forType(type));
        if (value == null) {
            cell.setBlank();
            return;
        }
        switch (type) {
            case TEXT -> cell.setCellValue(String.valueOf(value));
            case INTEGER -> cell.setCellValue(((Number) value).longValue());
            case DECIMAL, MONEY, PERCENT -> cell.setCellValue(toDouble(value));
            case DATETIME -> cell.setCellValue(LocalDateTime.ofInstant((Instant) value, ZoneOffset.UTC));
        }
    }

    private static double toDouble(Object value) {
        return value instanceof BigDecimal bd ? bd.doubleValue() : ((Number) value).doubleValue();
    }

    private static int widthFor(ColumnType type) {
        return switch (type) {
            case TEXT -> 28 * 256;
            case DATETIME -> 22 * 256;
            case MONEY, DECIMAL -> 16 * 256;
            case INTEGER, PERCENT -> 14 * 256;
        };
    }

    private static String safeSheetName(String raw, int index) {
        String base = (raw == null || raw.isBlank()) ? ("Sheet" + (index + 1)) : raw;
        return WorkbookUtil.createSafeSheetName(base);
    }
}
