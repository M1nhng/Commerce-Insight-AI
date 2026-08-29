package com.commerceinsight.export.excel;

import com.commerceinsight.export.dto.ColumnType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * ExcelStyleHelper — builds and caches the handful of {@link CellStyle}s used by
 * {@link ExcelExportWriter}. One instance per workbook (POI styles are
 * workbook-scoped and must be reused, never created per cell).
 */
final class ExcelStyleHelper {

    private final CellStyle titleStyle;
    private final CellStyle metaStyle;
    private final CellStyle headerStyle;
    private final CellStyle textStyle;
    private final CellStyle integerStyle;
    private final CellStyle decimalStyle;
    private final CellStyle moneyStyle;
    private final CellStyle percentStyle;
    private final CellStyle dateTimeStyle;

    ExcelStyleHelper(Workbook workbook) {
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        Font metaFont = workbook.createFont();
        metaFont.setItalic(true);
        metaFont.setFontHeightInPoints((short) 10);
        metaFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        this.titleStyle = workbook.createCellStyle();
        this.titleStyle.setFont(titleFont);

        this.metaStyle = workbook.createCellStyle();
        this.metaStyle.setFont(metaFont);

        this.headerStyle = workbook.createCellStyle();
        this.headerStyle.setFont(headerFont);
        this.headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        this.headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.headerStyle.setAlignment(HorizontalAlignment.LEFT);
        this.headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyThinBorder(this.headerStyle);

        short intFmt = workbook.createDataFormat().getFormat("#,##0");
        short decFmt = workbook.createDataFormat().getFormat("#,##0.00");
        short pctFmt = workbook.createDataFormat().getFormat("0.00");
        short dtFmt = workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss");

        this.textStyle = workbook.createCellStyle();
        applyThinBorder(this.textStyle);

        this.integerStyle = workbook.createCellStyle();
        this.integerStyle.setDataFormat(intFmt);
        applyThinBorder(this.integerStyle);

        this.decimalStyle = workbook.createCellStyle();
        this.decimalStyle.setDataFormat(decFmt);
        applyThinBorder(this.decimalStyle);

        this.moneyStyle = workbook.createCellStyle();
        this.moneyStyle.setDataFormat(decFmt);
        applyThinBorder(this.moneyStyle);

        this.percentStyle = workbook.createCellStyle();
        this.percentStyle.setDataFormat(pctFmt);
        applyThinBorder(this.percentStyle);

        this.dateTimeStyle = workbook.createCellStyle();
        this.dateTimeStyle.setDataFormat(dtFmt);
        applyThinBorder(this.dateTimeStyle);
    }

    private static void applyThinBorder(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    CellStyle title()  { return titleStyle; }
    CellStyle meta()   { return metaStyle; }
    CellStyle header() { return headerStyle; }

    CellStyle forType(ColumnType type) {
        return switch (type) {
            case TEXT -> textStyle;
            case INTEGER -> integerStyle;
            case DECIMAL -> decimalStyle;
            case MONEY -> moneyStyle;
            case PERCENT -> percentStyle;
            case DATETIME -> dateTimeStyle;
        };
    }
}
