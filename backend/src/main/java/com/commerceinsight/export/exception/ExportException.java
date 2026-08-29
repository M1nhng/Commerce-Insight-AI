package com.commerceinsight.export.exception;

import com.commerceinsight.shared.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ExportException — raised by the export module for request-level problems
 * (bad format, invalid date range, row-limit exceeded) and for file-generation
 * failures.
 *
 * <p>Mapped to an HTTP response by {@code GlobalExceptionHandler} in the same
 * style as {@code ImportException}. Each instance carries an {@link ErrorCode}
 * and the {@link HttpStatus} the handler should return.
 */
@Getter
public class ExportException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ExportException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ExportException(ErrorCode errorCode, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    // ── Factories ────────────────────────────────────────────────────────────

    /** HTTP 400 — unrecognised {@code format} parameter. */
    public static ExportException invalidFormat(String raw) {
        return new ExportException(
                ErrorCode.EXPORT_INVALID_FORMAT,
                HttpStatus.BAD_REQUEST,
                "Unsupported export format" + (raw == null || raw.isBlank() ? "" : " '" + raw.trim() + "'")
                        + ". Use PDF or XLSX.");
    }

    /** HTTP 400 — {@code dateFrom} is after {@code dateTo}. */
    public static ExportException invalidDateRange() {
        return new ExportException(
                ErrorCode.EXPORT_INVALID_DATE_RANGE,
                HttpStatus.BAD_REQUEST,
                "dateFrom must not be after dateTo");
    }

    /** HTTP 422 — the result set would exceed the configured row cap. */
    public static ExportException rowLimitExceeded(int maxRows) {
        return new ExportException(
                ErrorCode.EXPORT_ROW_LIMIT_EXCEEDED,
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Export exceeds the maximum allowed row count of " + maxRows + ".");
    }

    /** HTTP 500 — the workbook / PDF could not be produced. */
    public static ExportException generationFailed(Throwable cause) {
        return new ExportException(
                ErrorCode.EXPORT_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate the export. Please try again.",
                cause);
    }
}
