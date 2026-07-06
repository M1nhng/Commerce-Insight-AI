package com.commerceinsight.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * ApiResponse — standard response envelope for ALL API endpoints.
 *
 * <p>Architecture Rule: Every controller method MUST return
 * {@code ResponseEntity<ApiResponse<T>>}. No naked DTOs, no raw entities.
 *
 * <p>Success shape:
 * <pre>
 * {
 *   "success": true,
 *   "data": { ... },
 *   "message": "Resources retrieved successfully",
 *   "timestamp": "2026-07-06T10:00:00Z"
 * }
 * </pre>
 *
 * <p>Error shape:
 * <pre>
 * {
 *   "success": false,
 *   "error": { "code": "...", "message": "...", "details": null },
 *   "timestamp": "2026-07-06T10:00:00Z"
 * }
 * </pre>
 *
 * @param <T> the type of the response data
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final ErrorResponse error;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, String message, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
        this.timestamp = Instant.now();
    }

    // ── Factory Methods ──────────────────────────────────────────────────

    /**
     * Create a successful response with data and a custom message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /**
     * Create a successful response with data and the default "OK" message.
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "OK", null);
    }

    /**
     * Create a successful response with no data body (e.g., for 204 responses).
     */
    public static <Void> ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, null, "No Content", null);
    }

    /**
     * Create an error response from an {@link ErrorResponse} object.
     */
    public static <T> ApiResponse<T> error(ErrorResponse errorResponse) {
        return new ApiResponse<>(false, null, null, errorResponse);
    }
}
