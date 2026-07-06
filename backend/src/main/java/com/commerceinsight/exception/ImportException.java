package com.commerceinsight.exception;

import lombok.Getter;

/**
 * ImportException — thrown when a CSV or Excel import operation fails validation.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity via {@link GlobalExceptionHandler}.
 */
@Getter
public class ImportException extends RuntimeException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
