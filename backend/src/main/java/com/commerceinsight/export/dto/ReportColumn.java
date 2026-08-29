package com.commerceinsight.export.dto;

/**
 * ReportColumn — one column definition in a {@link ReportTable}: a header label
 * plus the {@link ColumnType} that tells the writers how to format its cells.
 */
public record ReportColumn(String header, ColumnType type) {

    public static ReportColumn text(String header)     { return new ReportColumn(header, ColumnType.TEXT); }
    public static ReportColumn integer(String header)   { return new ReportColumn(header, ColumnType.INTEGER); }
    public static ReportColumn decimal(String header)   { return new ReportColumn(header, ColumnType.DECIMAL); }
    public static ReportColumn money(String header)     { return new ReportColumn(header, ColumnType.MONEY); }
    public static ReportColumn percent(String header)   { return new ReportColumn(header, ColumnType.PERCENT); }
    public static ReportColumn dateTime(String header)  { return new ReportColumn(header, ColumnType.DATETIME); }
}
