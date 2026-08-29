package com.commerceinsight.export.pdf;

import com.commerceinsight.export.dto.ColumnType;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;

import java.awt.Color;

/**
 * PdfStyleHelper — shared fonts and cell factories for {@link PdfExportWriter}.
 * Keeps font sizes and colours in one place so every export PDF looks the same.
 */
final class PdfStyleHelper {

    static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(20, 20, 20));
    static final Font META_FONT = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new Color(110, 110, 110));
    static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(20, 20, 20));
    static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(130, 130, 130));

    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(30, 30, 30));

    private static final Color HEADER_BG = new Color(31, 59, 110);
    private static final Color ROW_ALT_BG = new Color(244, 246, 250);

    private PdfStyleHelper() {}

    static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        return cell;
    }

    static PdfPCell bodyCell(String text, ColumnType type, boolean altRow) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, BODY_FONT));
        cell.setHorizontalAlignment(isNumeric(type) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        if (altRow) {
            cell.setBackgroundColor(ROW_ALT_BG);
        }
        return cell;
    }

    /** Relative column width weight by type — text columns get more room. */
    static float widthWeight(ColumnType type) {
        return switch (type) {
            case TEXT -> 2.4f;
            case DATETIME -> 1.6f;
            case MONEY, DECIMAL -> 1.2f;
            case INTEGER, PERCENT -> 1.0f;
        };
    }

    private static boolean isNumeric(ColumnType type) {
        return type == ColumnType.INTEGER || type == ColumnType.DECIMAL
                || type == ColumnType.MONEY || type == ColumnType.PERCENT;
    }
}
