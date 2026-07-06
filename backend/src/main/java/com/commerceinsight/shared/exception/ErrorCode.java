package com.commerceinsight.shared.exception;

/**
 * ErrorCode — application-wide error code enumeration.
 *
 * <p>Every application exception maps to one of these codes.
 * The code is included in the {@link com.commerceinsight.shared.dto.ErrorResponse}
 * sent to the client.
 *
 * <p>Naming convention: {DOMAIN}_{CONDITION} — uppercase snake_case.
 */
public enum ErrorCode {

    // ── Generic ────────────────────────────────────────────────────────────
    VALIDATION_ERROR,
    INTERNAL_ERROR,
    RESOURCE_NOT_FOUND,
    ACCESS_DENIED,
    AUTHENTICATION_REQUIRED,

    // ── Authentication ─────────────────────────────────────────────────────
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    REFRESH_TOKEN_INVALID,
    REFRESH_TOKEN_EXPIRED,
    REFRESH_TOKEN_REUSE_DETECTED,
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    ACCOUNT_LOCKED,

    // ── User ───────────────────────────────────────────────────────────────
    USER_NOT_FOUND,
    USER_EMAIL_ALREADY_EXISTS,
    USER_CANNOT_CHANGE_OWN_ROLE,

    // ── Product ────────────────────────────────────────────────────────────
    PRODUCT_NOT_FOUND,
    DUPLICATE_SKU,

    // ── Category ───────────────────────────────────────────────────────────
    CATEGORY_NOT_FOUND,
    CATEGORY_HAS_PRODUCTS,
    CATEGORY_CIRCULAR_REFERENCE,

    // ── Customer ───────────────────────────────────────────────────────────
    CUSTOMER_NOT_FOUND,

    // ── Order ──────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    INSUFFICIENT_STOCK,

    // ── Inventory ──────────────────────────────────────────────────────────
    INVENTORY_NOT_FOUND,

    // ── Import / Export ────────────────────────────────────────────────────
    IMPORT_VALIDATION_FAILED,
    IMPORT_FILE_TOO_LARGE,
    IMPORT_UNSUPPORTED_FORMAT,
    EXPORT_FAILED,

    // ── AI ─────────────────────────────────────────────────────────────────
    AI_PROVIDER_UNAVAILABLE,
    AI_RATE_LIMIT_EXCEEDED,
    CONVERSATION_NOT_FOUND,

    // ── MCP ────────────────────────────────────────────────────────────────
    MCP_INVALID_API_KEY,
}
