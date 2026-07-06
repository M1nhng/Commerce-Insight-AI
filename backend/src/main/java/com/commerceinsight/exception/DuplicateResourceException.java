package com.commerceinsight.exception;

import com.commerceinsight.shared.exception.ErrorCode;
import lombok.Getter;

/**
 * DuplicateResourceException — thrown when attempting to create a resource that violates a
 * uniqueness constraint (e.g., duplicate SKU, duplicate email).
 *
 * <p>Maps to HTTP 409 Conflict via {@link GlobalExceptionHandler}.
 */
@Getter
public class DuplicateResourceException extends RuntimeException {

    private final ErrorCode errorCode;

    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public static DuplicateResourceException sku(String sku) {
        return new DuplicateResourceException(ErrorCode.DUPLICATE_SKU,
                "A product with SKU '" + sku + "' already exists");
    }

    public static DuplicateResourceException email(String email) {
        return new DuplicateResourceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS,
                "An account with email '" + email + "' already exists");
    }
}
