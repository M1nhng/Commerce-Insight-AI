package com.commerceinsight.exception;

import com.commerceinsight.shared.exception.ErrorCode;
import lombok.Getter;

/**
 * ResourceNotFoundException — thrown when a requested entity does not exist in the database.
 *
 * <p>Maps to HTTP 404 Not Found via {@link GlobalExceptionHandler}.
 *
 * <p>Usage:
 * <pre>
 * throw new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND,
 *     "Product with ID '" + id + "' was not found");
 * </pre>
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // ── Factory convenience methods ───────────────────────────────────────

    public static ResourceNotFoundException product(Object id) {
        return new ResourceNotFoundException(ErrorCode.PRODUCT_NOT_FOUND,
                "Product with ID '" + id + "' was not found");
    }

    public static ResourceNotFoundException user(Object id) {
        return new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                "User with ID '" + id + "' was not found");
    }

    public static ResourceNotFoundException category(Object id) {
        return new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND,
                "Category with ID '" + id + "' was not found");
    }

    public static ResourceNotFoundException customer(Object id) {
        return new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                "Customer with ID '" + id + "' was not found");
    }

    public static ResourceNotFoundException order(Object id) {
        return new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND,
                "Order with ID '" + id + "' was not found");
    }
}
