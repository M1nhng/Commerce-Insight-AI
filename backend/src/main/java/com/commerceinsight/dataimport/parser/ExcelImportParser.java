package com.commerceinsight.dataimport.parser;

import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * ExcelImportParser — parses XLSX files into {@link ParsedRow} objects using Apache POI.
 *
 * <p>Handles:
 * <ul>
 *   <li>String cells</li>
 *   <li>Numeric cells (integer and decimal)</li>
 *   <li>Date cells (converted to ISO date string yyyy-MM-dd)</li>
 *   <li>Boolean cells</li>
 *   <li>Formula cells (evaluated to string value)</li>
 *   <li>Blank / null cells → empty string</li>
 *   <li>Configurable maximum row count</li>
 *   <li>Missing or invalid headers</li>
 * </ul>
 *
 * <p>Only supports .xlsx format. .xls is rejected at file validation stage.
 */
@Slf4j
@Component
public class ExcelImportParser implements ImportParser {

    @Override
    public List<ParsedRow> parse(InputStream inputStream, String[] expectedHeaders, int maxRows) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ImportException(
                        "[%s] Excel file contains no sheets".formatted(ImportValidationCode.EMPTY_FILE));
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new ImportException(
                        "[%s] Excel sheet is empty".formatted(ImportValidationCode.EMPTY_FILE));
            }

            // First row = header
            Row headerRow = rowIterator.next();
            Map<Integer, String> columnIndexToName = parseHeaders(headerRow, expectedHeaders);

            List<ParsedRow> rows = new ArrayList<>();
            int rowNumber = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                // Skip fully empty rows
                if (isRowEmpty(row)) continue;

                rowNumber++;
                if (rowNumber > maxRows) {
                    throw new ImportException(
                            "[%s] File exceeds the maximum allowed row count of %d"
                                    .formatted(ImportValidationCode.ROW_LIMIT_EXCEEDED, maxRows));
                }

                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<Integer, String> entry : columnIndexToName.entrySet()) {
                    Cell cell = row.getCell(entry.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.put(entry.getValue(), cellToString(cell));
                }
                rows.add(new ParsedRow(rowNumber, values));
            }

            log.debug("Excel parsed: {} rows", rows.size());
            return rows;

        } catch (ImportException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportException(
                    "[%s] Failed to read Excel file: %s"
                            .formatted(ImportValidationCode.INVALID_HEADER, e.getMessage()), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<Integer, String> parseHeaders(Row headerRow, String[] expectedHeaders) {
        Map<Integer, String> indexToName = new LinkedHashMap<>();
        if (headerRow == null) {
            throw new ImportException(
                    "[%s] Excel file has no header row".formatted(ImportValidationCode.INVALID_HEADER));
        }

        for (Cell cell : headerRow) {
            String name = cellToString(cell).toLowerCase().trim();
            if (!name.isEmpty()) {
                indexToName.put(cell.getColumnIndex(), name);
            }
        }

        if (indexToName.isEmpty()) {
            throw new ImportException(
                    "[%s] Excel header row is empty".formatted(ImportValidationCode.INVALID_HEADER));
        }

        // Validate required headers
        List<String> missing = new ArrayList<>();
        for (String req : expectedHeaders) {
            if (!indexToName.containsValue(req.toLowerCase().trim())) {
                missing.add(req);
            }
        }
        if (!missing.isEmpty()) {
            throw new ImportException(
                    "[%s] Excel file is missing required columns: %s"
                            .formatted(ImportValidationCode.INVALID_HEADER, String.join(", ", missing)));
        }

        return indexToName;
    }

    /**
     * Converts any POI Cell to a String representation.
     * Formula cells are evaluated to their cached value.
     * Numeric cells that are whole numbers (e.g. 100.0) are returned without decimal.
     */
    private String cellToString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Return ISO date string
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double val = cell.getNumericCellValue();
                // Return integer representation for whole numbers
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                // Use cached value — don't re-evaluate (avoids dependency on formula engine)
                CellType cached = cell.getCachedFormulaResultType();
                if (cached == CellType.STRING)  yield cell.getStringCellValue().trim();
                if (cached == CellType.NUMERIC) {
                    double val = cell.getNumericCellValue();
                    if (val == Math.floor(val) && !Double.isInfinite(val)) yield String.valueOf((long) val);
                    yield String.valueOf(val);
                }
                yield "";
            }
            case BLANK, _NONE, ERROR -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = cellToString(cell);
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }
}
