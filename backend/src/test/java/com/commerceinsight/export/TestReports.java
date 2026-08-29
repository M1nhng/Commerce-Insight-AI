package com.commerceinsight.export;

import com.commerceinsight.export.dto.ColumnType;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared fixtures / helpers for export tests.
 */
public final class TestReports {

    public static final Instant TS = Instant.parse("2026-08-15T09:30:00Z");

    private TestReports() {}

    /** A small multi-type single-table document with one null cell. */
    public static ReportDocument sample() {
        List<ReportColumn> columns = List.of(
                new ReportColumn("Name", ColumnType.TEXT),
                new ReportColumn("Amount", ColumnType.MONEY),
                new ReportColumn("Count", ColumnType.INTEGER),
                new ReportColumn("Rate (%)", ColumnType.PERCENT),
                new ReportColumn("When (UTC)", ColumnType.DATETIME));

        List<List<Object>> rows = new ArrayList<>();
        rows.add(newRow("Alpha widget", new BigDecimal("199.99"), 3L, new BigDecimal("42.50"), TS));
        rows.add(newRow(null, new BigDecimal("0.00"), 0L, new BigDecimal("0.00"), TS));

        return ReportDocument.single("Sample Export", TS, new ReportTable("Test", columns, rows));
    }

    public static ReportDocument twoTables() {
        ReportTable a = new ReportTable("First",
                List.of(new ReportColumn("K", ColumnType.TEXT)),
                List.of(newRow("v1")));
        ReportTable b = new ReportTable("Second",
                List.of(new ReportColumn("K", ColumnType.TEXT)),
                List.of(newRow("v2")));
        return new ReportDocument("Multi Export", TS, List.of(a, b));
    }

    public static List<Object> newRow(Object... values) {
        List<Object> out = new ArrayList<>(values.length);
        for (Object v : values) {
            out.add(v);
        }
        return out;
    }

    public static Workbook openXlsx(byte[] bytes) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(bytes));
    }

    public static String pdfText(byte[] bytes) throws Exception {
        PdfReader reader = new PdfReader(bytes);
        try {
            StringBuilder sb = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page)).append('\n');
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }
}
