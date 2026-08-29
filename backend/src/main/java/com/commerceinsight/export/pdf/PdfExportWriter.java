package com.commerceinsight.export.pdf;

import com.commerceinsight.export.dto.ColumnType;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.exception.ExportException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PdfExportWriter — renders a format-neutral {@link ReportDocument} into a PDF.
 *
 * <p>Reusable across every report type; contains no business logic. Always
 * landscape A4 (export tables are wide), with a title, a generated-at line,
 * repeating table header rows and a "Page N" footer.
 */
@Component
public class PdfExportWriter {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] write(ReportDocument document) {
        Document pdf = new Document(PageSize.A4.rotate(), 36, 36, 46, 40);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(pdf, out);
            writer.setPageEvent(new PageNumberFooter());
            pdf.open();

            Paragraph title = new Paragraph(document.title(), PdfStyleHelper.TITLE_FONT);
            title.setSpacingAfter(2f);
            pdf.add(title);

            Instant generatedAt = document.generatedAt() != null ? document.generatedAt() : Instant.now();
            Paragraph meta = new Paragraph("Generated: " + TS.format(generatedAt), PdfStyleHelper.META_FONT);
            meta.setSpacingAfter(10f);
            pdf.add(meta);

            List<ReportTable> tables = document.tables();
            for (int i = 0; i < tables.size(); i++) {
                ReportTable table = tables.get(i);
                if (tables.size() > 1) {
                    Paragraph section = new Paragraph(
                            (table.name() == null ? "Section " + (i + 1) : table.name()),
                            PdfStyleHelper.SECTION_FONT);
                    section.setSpacingBefore(i == 0 ? 0f : 12f);
                    section.setSpacingAfter(4f);
                    pdf.add(section);
                }
                pdf.add(buildTable(table));
            }

            pdf.close();
            return out.toByteArray();
        } catch (Exception ex) {
            if (pdf.isOpen()) {
                try { pdf.close(); } catch (Exception ignored) { /* already failing */ }
            }
            throw ExportException.generationFailed(ex);
        }
    }

    private PdfPTable buildTable(ReportTable table) throws Exception {
        List<ReportColumn> columns = table.columns();
        int colCount = columns.size();

        PdfPTable pdfTable = new PdfPTable(colCount);
        pdfTable.setWidthPercentage(100f);
        pdfTable.setHeaderRows(1);

        float[] weights = new float[colCount];
        for (int c = 0; c < colCount; c++) {
            weights[c] = PdfStyleHelper.widthWeight(columns.get(c).type());
        }
        pdfTable.setWidths(weights);

        for (ReportColumn column : columns) {
            pdfTable.addCell(PdfStyleHelper.headerCell(column.header()));
        }

        List<List<Object>> rows = table.rows();
        for (int r = 0; r < rows.size(); r++) {
            List<Object> values = rows.get(r);
            boolean altRow = (r & 1) == 1;
            for (int c = 0; c < colCount; c++) {
                Object value = c < values.size() ? values.get(c) : null;
                pdfTable.addCell(PdfStyleHelper.bodyCell(render(columns.get(c).type(), value), columns.get(c).type(), altRow));
            }
        }
        return pdfTable;
    }

    private static String render(ColumnType type, Object value) {
        if (value == null) {
            return "";
        }
        return switch (type) {
            case TEXT -> String.valueOf(value);
            case INTEGER -> new DecimalFormat("#,##0").format(((Number) value).longValue());
            case DECIMAL, MONEY -> new DecimalFormat("#,##0.00").format(scale2(value));
            case PERCENT -> new DecimalFormat("0.00").format(scale2(value));
            case DATETIME -> TS.format((Instant) value);
        };
    }

    private static BigDecimal scale2(Object value) {
        BigDecimal bd = value instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) value).doubleValue());
        return bd.setScale(2, RoundingMode.HALF_UP);
    }

    /** Writes a centred "Page N" at the bottom of every page. */
    private static final class PageNumberFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(
                    writer.getDirectContent(),
                    Element.ALIGN_CENTER,
                    new Phrase("Page " + writer.getPageNumber(), PdfStyleHelper.FOOTER_FONT),
                    (document.left() + document.right()) / 2,
                    document.bottom() - 18,
                    0);
        }
    }
}
