package com.commerceinsight.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * ErrorResponse — structured error payload embedded inside {@link ApiResponse}.
 *
 * <p>Single-error shape (code + message):
 * <pre>
 * {
 *   "code": "PRODUCT_NOT_FOUND",
 *   "message": "Product with ID '...' was not found"
 * }
 * </pre>
 *
 * <p>Validation-error shape (with details list):
 * <pre>
 * {
 *   "code": "VALIDATION_ERROR",
 *   "message": "Request validation failed",
 *   "details": [
 *     { "field": "name", "message": "must not be blank" }
 *   ]
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Application-specific error code (e.g., "PRODUCT_NOT_FOUND"). */
    private final String code;

    /** Human-readable error message. Safe to expose to the client. */
    private final String message;

    /** Optional list of field-level validation errors. */
    private final List<FieldError> details;

    // ── Factory Methods ──────────────────────────────────────────────────

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder().code(code).message(message).build();
    }

    public static ErrorResponse withDetails(String code, String message, List<FieldError> details) {
        return ErrorResponse.builder().code(code).message(message).details(details).build();
    }

    // ── Nested Types ─────────────────────────────────────────────────────

    /**
     * A single field-level validation error.
     */
    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;

        public static FieldError of(String field, String message) {
            return FieldError.builder().field(field).message(message).build();
        }
    }
}
