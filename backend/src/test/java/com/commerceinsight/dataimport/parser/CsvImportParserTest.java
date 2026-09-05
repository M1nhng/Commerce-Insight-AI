package com.commerceinsight.dataimport.parser;

import com.commerceinsight.exception.ImportException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * CsvImportParserTest — unit tests for {@link CsvImportParser}.
 */
@DisplayName("CsvImportParser Unit Tests")
class CsvImportParserTest {

    private final CsvImportParser parser = new CsvImportParser();

    private static final String[] HEADERS = {"sku", "name", "price"};

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Parse valid CSV with all required headers — returns correct rows")
    void parseValidCsv_returnsRows() {
        String csv = "sku,name,price\nSKU-001,Widget,100.00\nSKU-002,Gadget,200.50\n";
        InputStream is = toStream(csv);

        List<ParsedRow> rows = parser.parse(is, HEADERS, 100);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getRowNumber()).isEqualTo(1);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
        assertThat(rows.get(0).get("name")).isEqualTo("Widget");
        assertThat(rows.get(0).get("price")).isEqualTo("100.00");

        assertThat(rows.get(1).getRowNumber()).isEqualTo(2);
        assertThat(rows.get(1).get("sku")).isEqualTo("SKU-002");
    }

    @Test
    @DisplayName("Parse CSV with BOM — BOM stripped, header parsed correctly")
    void parseWithBom_bomStripped() {
        // UTF-8 BOM = EF BB BF
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        String csv = "sku,name,price\nSKU-001,Widget,100.00\n";
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(csvBytes, 0, combined, bom.length, csvBytes.length);

        InputStream is = new ByteArrayInputStream(combined);
        List<ParsedRow> rows = parser.parse(is, HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
    }

    @Test
    @DisplayName("Parse CSV, no BOM, on a stream that does NOT support mark/reset — header parsed correctly")
    void parseNoBomOnNonResettableStream_headerNotCorrupted() {
        // Regression test: a multipart upload's stream (backed by a temp file once
        // Spring writes it to disk) does not support mark/reset. stripBom() must not
        // depend on mark/reset succeeding, or it silently eats the first 3 header
        // bytes on every such upload. ByteArrayInputStream (used by every other test
        // here) supports mark/reset and would never have caught this.
        String csv = "sku,name,price\nSKU-001,Widget,100.00\n";
        InputStream nonResettable = new java.io.FilterInputStream(toStream(csv)) {
            @Override
            public boolean markSupported() {
                return false;
            }

            @Override
            public synchronized void reset() throws IOException {
                throw new IOException("mark/reset not supported");
            }
        };

        List<ParsedRow> rows = parser.parse(nonResettable, HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
        assertThat(rows.get(0).get("name")).isEqualTo("Widget");
    }

    @Test
    @DisplayName("Parse CSV with quoted commas — quoted value preserved")
    void parseQuotedCommas_valuePreserved() {
        String csv = "sku,name,price\nSKU-001,\"Widget, Premium\",100.00\n";
        List<ParsedRow> rows = parser.parse(toStream(csv), HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("name")).isEqualTo("Widget, Premium");
    }

    @Test
    @DisplayName("Parse CSV with blank optional field — returns empty string")
    void parseBlankOptionalField_returnsEmpty() {
        String csv = "sku,name,price,description\nSKU-001,Widget,100.00,\n";
        List<ParsedRow> rows = parser.parse(toStream(csv), HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("description")).isEmpty();
        assertThat(rows.get(0).hasValue("description")).isFalse();
    }

    @Test
    @DisplayName("Parse CSV with extra columns beyond required headers — extra columns present in row")
    void parseExtraColumns_extraColumnsAvailable() {
        String csv = "sku,name,price,imageurl\nSKU-001,Widget,100.00,http://img.example.com/1.jpg\n";
        List<ParsedRow> rows = parser.parse(toStream(csv), HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("imageurl")).isEqualTo("http://img.example.com/1.jpg");
    }

    @Test
    @DisplayName("Parse CSV case-insensitive headers — values accessible by lowercase key")
    void parseCaseInsensitiveHeaders_accessible() {
        String csv = "SKU,Name,Price\nSKU-001,Widget,100.00\n";
        List<ParsedRow> rows = parser.parse(toStream(csv), HEADERS, 100);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
        assertThat(rows.get(0).get("name")).isEqualTo("Widget");
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Missing required header — throws ImportException with INVALID_HEADER")
    void missingRequiredHeader_throwsImportException() {
        String csv = "name,price\nWidget,100.00\n";  // missing 'sku'

        assertThatThrownBy(() -> parser.parse(toStream(csv), HEADERS, 100))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("INVALID_HEADER")
                .hasMessageContaining("sku");
    }

    @Test
    @DisplayName("Row limit exceeded — throws ImportException with ROW_LIMIT_EXCEEDED")
    void rowLimitExceeded_throwsImportException() {
        String csv = "sku,name,price\nSKU-001,W,1.00\nSKU-002,W,2.00\nSKU-003,W,3.00\n";

        assertThatThrownBy(() -> parser.parse(toStream(csv), HEADERS, 2))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("ROW_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Empty CSV (header only) — returns empty list")
    void headerOnlyCsv_returnsEmptyList() {
        String csv = "sku,name,price\n";
        List<ParsedRow> rows = parser.parse(toStream(csv), HEADERS, 100);
        assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("CSV with CRLF line endings — parsed correctly")
    void crlfLineEndings_parsedCorrectly() {
        String csv = "sku,name,price\r\nSKU-001,Widget,100.00\r\n";
        List<ParsedRow> rows = parser.parse(toStream(csv), HEADERS, 100);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("sku")).isEqualTo("SKU-001");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private InputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
