package com.commerceinsight.dataimport.parser;

import com.commerceinsight.exception.ImportException;

import java.io.InputStream;
import java.util.List;

/**
 * ImportParser — abstraction for reading tabular data from uploaded files.
 *
 * <p>Implementations must handle their format's edge cases internally
 * (BOM, quoted values, numeric cells, formula cells, etc.)
 * so that domain import services only work with {@link ParsedRow} objects.
 *
 * <p>Architecture Rule: implementations must NOT load the entire file into
 * memory at once when the format supports streaming. Prefer streaming where
 * practical.
 */
public interface ImportParser {

    /**
     * Parses a file into a list of {@link ParsedRow} objects.
     *
     * <p>The first row is expected to be the header row.
     * Column names are normalized to lowercase, trimmed strings.
     *
     * @param inputStream  the raw file byte stream (must not be null)
     * @param expectedHeaders the header names expected in the file, in any order.
     *                        An {@link ImportException} is thrown if any required header is absent.
     * @param maxRows      the maximum number of data rows to parse before rejecting the file
     * @return list of parsed rows (header excluded), in file order
     * @throws ImportException if the file cannot be parsed, headers are wrong, or row limit is exceeded
     */
    List<ParsedRow> parse(InputStream inputStream, String[] expectedHeaders, int maxRows);
}
