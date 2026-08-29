package com.commerceinsight.export.dto;

/**
 * ColumnType — how a report column's values should be formatted by the
 * Excel / PDF writers. Purely presentational; carries no business meaning.
 */
public enum ColumnType {

    /** Plain string. */
    TEXT,

    /** Whole number, thousands-separated. */
    INTEGER,

    /** Decimal number, 2 fraction digits, thousands-separated. */
    DECIMAL,

    /** Monetary amount, 2 fraction digits. The currency is carried in a sibling TEXT column. */
    MONEY,

    /** A percentage value already expressed on a 0–100 scale. */
    PERCENT,

    /** An {@link java.time.Instant} rendered as {@code yyyy-MM-dd HH:mm:ss 'UTC'}. */
    DATETIME
}
