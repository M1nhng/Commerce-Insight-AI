package com.commerceinsight.dataimport.parser;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ParsedRow — a single data row extracted from a CSV or Excel file.
 *
 * <p>All cell values are represented as trimmed {@code String}s regardless of
 * source format. Null cell values become empty strings ("").
 *
 * <p>The map keys are the normalized (lowercase, trimmed) column header names
 * from the file's header row.
 */
@Getter
public class ParsedRow {

    /** 1-based row number in the file (1 = first data row, after header). */
    private final int rowNumber;

    /** Column name → raw string value map. Keys are lowercase header names. */
    private final Map<String, String> values;

    public ParsedRow(int rowNumber, Map<String, String> values) {
        this.rowNumber = rowNumber;
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    /**
     * Returns the trimmed value for the given column name (case-insensitive).
     * Returns an empty string if the column is absent.
     */
    public String get(String column) {
        return values.getOrDefault(column.toLowerCase().trim(), "").trim();
    }

    /**
     * Returns whether the given column has a non-blank value.
     */
    public boolean hasValue(String column) {
        return !get(column).isEmpty();
    }
}
