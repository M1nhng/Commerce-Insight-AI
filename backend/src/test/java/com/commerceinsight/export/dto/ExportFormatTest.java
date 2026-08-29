package com.commerceinsight.export.dto;

import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExportFormat")
class ExportFormatTest {

    @ParameterizedTest
    @ValueSource(strings = {"xlsx", "XLSX", "Xlsx", "  xlsx  "})
    @DisplayName("accepts XLSX in any case / with padding")
    void acceptsXlsx(String raw) {
        assertThat(ExportFormat.from(raw)).isEqualTo(ExportFormat.XLSX);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "PDF", "Pdf", " pdf"})
    @DisplayName("accepts PDF in any case / with padding")
    void acceptsPdf(String raw) {
        assertThat(ExportFormat.from(raw)).isEqualTo(ExportFormat.PDF);
    }

    @ParameterizedTest
    @ValueSource(strings = {"csv", "docx", "json", "xls", "html"})
    @DisplayName("rejects any unsupported format with a 400 EXPORT_INVALID_FORMAT")
    void rejectsUnsupported(String raw) {
        assertThatThrownBy(() -> ExportFormat.from(raw))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> {
                    ExportException e = (ExportException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXPORT_INVALID_FORMAT);
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("PDF").contains("XLSX");
                });
    }

    @Test
    @DisplayName("null and blank are rejected, never silently defaulted")
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> ExportFormat.from(null)).isInstanceOf(ExportException.class);
        assertThatThrownBy(() -> ExportFormat.from("")).isInstanceOf(ExportException.class);
        assertThatThrownBy(() -> ExportFormat.from("   ")).isInstanceOf(ExportException.class);
    }

    @Test
    @DisplayName("exposes the correct content type and file extension")
    void metadata() {
        assertThat(ExportFormat.XLSX.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(ExportFormat.XLSX.fileExtension()).isEqualTo("xlsx");
        assertThat(ExportFormat.PDF.contentType()).isEqualTo("application/pdf");
        assertThat(ExportFormat.PDF.fileExtension()).isEqualTo("pdf");
    }
}
