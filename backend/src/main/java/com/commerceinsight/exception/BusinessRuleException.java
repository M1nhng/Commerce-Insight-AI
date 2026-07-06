package com.commerceinsight.exception;

import com.commerceinsight.shared.exception.ErrorCode;
import lombok.Getter;

/**
 * BusinessRuleException — thrown when a business rule is violated.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity via {@link GlobalExceptionHandler}.
 *
 * <p>Examples:
 * <ul>
 *   <li>Invalid order status transition (e.g., DELIVERED → PENDING)</li>
 *   <li>Deleting a category that still has products</li>
 *   <li>Stock cannot go negative</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
 *     "Cannot transition order from DELIVERED to PENDING");
 * </pre>
 */
@Getter
public class BusinessRuleException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessRuleException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
