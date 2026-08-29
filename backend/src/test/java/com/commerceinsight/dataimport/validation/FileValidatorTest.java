package com.commerceinsight.dataimport.validation;

import com.commerceinsight.dataimport.config.ImportProperties;
import com.commerceinsight.dataimport.domain.ImportFileType;
import com.commerceinsight.exception.ImportException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

/**
 * FileValidatorTest — unit tests for {@link FileValidator}.
 */
@DisplayName("FileValidator Unit Tests")
class FileValidatorTest {

    private FileValidator validator;

    @BeforeEach
    void setUp() {
        ImportProperties props = new ImportProperties();
        props.setMaxFileSizeMb(10);
        props.setMaxRows(5000);
        validator = new FileValidator(props);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid CSV file — returns ImportFileType.CSV")
    void validCsvFile_returnsCsv() {
        MultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", "sku,name,price\n".getBytes());

        ImportFileType result = validator.validate(file);

        assertThat(result).isEqualTo(ImportFileType.CSV);
    }

    @Test
    @DisplayName("Valid XLSX file — returns ImportFileType.XLSX")
    void validXlsxFile_returnsXlsx() {
        MultipartFile file = new MockMultipartFile(
                "file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{0x50, 0x4B, 0x03, 0x04}); // XLSX magic bytes

        ImportFileType result = validator.validate(file);

        assertThat(result).isEqualTo(ImportFileType.XLSX);
    }

    @Test
    @DisplayName("CSV with plain/text content type — accepted based on extension")
    void csvWithTextPlainContentType_accepted() {
        MultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/plain", "sku,name\n".getBytes());

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    // ── Error cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty file — throws ImportException with EMPTY_FILE")
    void emptyFile_throwsImportException() {
        MultipartFile file = new MockMultipartFile(
                "file", "products.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("EMPTY_FILE");
    }

    @Test
    @DisplayName("File too large — throws ImportException with FILE_TOO_LARGE")
    void fileTooLarge_throwsImportException() {
        ImportProperties smallProps = new ImportProperties();
        smallProps.setMaxFileSizeMb(1);
        FileValidator smallValidator = new FileValidator(smallProps);

        // 2MB of data
        byte[] data = new byte[2 * 1024 * 1024 + 1];
        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", data);

        assertThatThrownBy(() -> smallValidator.validate(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("FILE_TOO_LARGE");
    }

    @Test
    @DisplayName("Unsupported extension .pdf — throws ImportException with UNSUPPORTED_FILE_TYPE")
    void pdfExtension_throwsImportException() {
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "some content".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("UNSUPPORTED_FILE_TYPE")
                .hasMessageContaining(".pdf");
    }

    @Test
    @DisplayName("Unsupported extension .xls — throws ImportException with UNSUPPORTED_FILE_TYPE")
    void xlsExtension_throwsImportException() {
        MultipartFile file = new MockMultipartFile(
                "file", "products.xls", "application/vnd.ms-excel", "content".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("UNSUPPORTED_FILE_TYPE")
                .hasMessageContaining(".xls");
    }

    @Test
    @DisplayName("File with no extension — throws ImportException with UNSUPPORTED_FILE_TYPE")
    void noExtension_throwsImportException() {
        MultipartFile file = new MockMultipartFile(
                "file", "products", "text/csv", "sku,name\n".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("UNSUPPORTED_FILE_TYPE");
    }

    @Test
    @DisplayName("Null original filename — throws ImportException with UNSUPPORTED_FILE_TYPE")
    void nullFilename_throwsImportException() {
        MultipartFile file = new MockMultipartFile(
                "file", null, "text/csv", "sku,name\n".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ImportException.class)
                .hasMessageContaining("UNSUPPORTED_FILE_TYPE");
    }
}
