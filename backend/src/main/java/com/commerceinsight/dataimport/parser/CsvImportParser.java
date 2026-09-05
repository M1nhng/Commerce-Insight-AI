package com.commerceinsight.dataimport.parser;

import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.ImportException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CsvImportParser — parses CSV files into {@link ParsedRow} objects.
 *
 * <p>Handles:
 * <ul>
 *   <li>UTF-8 with and without BOM</li>
 *   <li>Quoted values (including commas inside quotes)</li>
 *   <li>CRLF and LF line endings</li>
 *   <li>Empty/blank fields → empty string</li>
 *   <li>Configurable maximum row count</li>
 *   <li>Missing or invalid headers</li>
 * </ul>
 *
 * <p>Uses Apache Commons CSV — never manually splits on commas.
 */
@Slf4j
@Component
public class CsvImportParser implements ImportParser {

    @Override
    public List<ParsedRow> parse(InputStream inputStream, String[] expectedHeaders, int maxRows) {
        Reader reader = stripBom(inputStream);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setNullString("")
                .build();

        try (CSVParser csvParser = new CSVParser(reader, format)) {

            // Validate headers
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            validateHeaders(headerMap, expectedHeaders);

            List<ParsedRow> rows = new ArrayList<>();
            int rowNumber = 0;

            for (CSVRecord record : csvParser) {
                rowNumber++;
                if (rowNumber > maxRows) {
                    throw new ImportException(
                            "[%s] File exceeds the maximum allowed row count of %d"
                                    .formatted(ImportValidationCode.ROW_LIMIT_EXCEEDED, maxRows));
                }

                Map<String, String> values = new LinkedHashMap<>();
                for (String header : headerMap.keySet()) {
                    String val = record.isMapped(header) ? record.get(header) : "";
                    values.put(header.toLowerCase().trim(), val == null ? "" : val.trim());
                }
                rows.add(new ParsedRow(rowNumber, values));
            }

            log.debug("CSV parsed: {} rows", rows.size());
            return rows;

        } catch (ImportException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportException(
                    "[%s] Failed to parse CSV file: %s".formatted(
                            ImportValidationCode.INVALID_HEADER, e.getMessage()), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Strips the UTF-8 BOM (EF BB BF) if present, so Commons CSV does not
     * include it as part of the first header name.
     *
     * <p>Wrapped in a {@link PushbackInputStream} rather than relying on
     * {@code mark}/{@code reset} on the caller's stream: a multipart file's
     * stream (backed by a temp file once Spring writes the upload to disk) does
     * not support {@code mark}/{@code reset}, so a bare {@code reset()} after
     * peeking 3 bytes silently swallowed the first 3 header bytes on every such
     * upload (e.g. turning {@code "sku,name,..."} into {@code ",name,..."}).
     * {@link PushbackInputStream} can always push bytes back, independent of
     * what the underlying stream supports.
     */
    private Reader stripBom(InputStream inputStream) {
        PushbackInputStream pushback = new PushbackInputStream(inputStream, 3);
        try {
            byte[] bom = new byte[3];
            int read = pushback.read(bom, 0, 3);
            boolean isBom = read == 3
                    && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF;
            if (!isBom && read > 0) {
                pushback.unread(bom, 0, read);
            }
            return new InputStreamReader(pushback, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new InputStreamReader(pushback, StandardCharsets.UTF_8);
        }
    }

    private void validateHeaders(Map<String, Integer> actualHeaders, String[] expected) {
        if (actualHeaders == null || actualHeaders.isEmpty()) {
            throw new ImportException(
                    "[%s] CSV file has no header row".formatted(ImportValidationCode.INVALID_HEADER));
        }
        Set<String> normalizedActual = new HashSet<>();
        for (String h : actualHeaders.keySet()) {
            normalizedActual.add(h.toLowerCase().trim());
        }
        List<String> missing = new ArrayList<>();
        for (String req : expected) {
            if (!normalizedActual.contains(req.toLowerCase().trim())) {
                missing.add(req);
            }
        }
        if (!missing.isEmpty()) {
            throw new ImportException(
                    "[%s] CSV file is missing required columns: %s"
                            .formatted(ImportValidationCode.INVALID_HEADER, String.join(", ", missing)));
        }
    }
}
