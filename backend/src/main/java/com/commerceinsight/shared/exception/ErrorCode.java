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
    RESOURCE_CONFLICT,
    ACCESS_DENIED,
    AUTHENTICATION_REQUIRED,
    RATE_LIMIT_EXCEEDED,
    UNSUPPORTED_MEDIA_TYPE,
    PAYLOAD_TOO_LARGE,

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
    CUSTOMER_CODE_ALREADY_EXISTS,
    CUSTOMER_EMAIL_ALREADY_EXISTS,
    CUSTOMER_GROUP_NOT_FOUND,
    CUSTOMER_GROUP_CODE_ALREADY_EXISTS,
    CUSTOMER_ADDRESS_NOT_FOUND,
    CUSTOMER_SEGMENT_NOT_FOUND,
    CUSTOMER_SEGMENT_CODE_ALREADY_EXISTS,
    DEFAULT_ADDRESS_CONFLICT,

    // ── Order ──────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND,
    ORDER_ITEM_INVALID,
    ORDER_ALREADY_CANCELLED,
    ORDER_CANNOT_CANCEL,
    ORDER_PAYMENT_NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    INSUFFICIENT_STOCK,
    PRODUCT_INACTIVE,
    CUSTOMER_INACTIVE,
    CUSTOMER_BLOCKED,

    // ── Inventory ──────────────────────────────────────────────────────────
    INVENTORY_NOT_FOUND,
    WAREHOUSE_NOT_FOUND,
    WAREHOUSE_CODE_ALREADY_EXISTS,
    WAREHOUSE_HAS_INVENTORY,
    NEGATIVE_STOCK_NOT_ALLOWED,
    SAME_WAREHOUSE_TRANSFER,
    STOCK_ADJUSTMENT_NOT_FOUND,
    STOCK_ADJUSTMENT_ALREADY_REVIEWED,

    // ── Import / Export ────────────────────────────────────────────────────────
    IMPORT_VALIDATION_FAILED,
    IMPORT_FILE_TOO_LARGE,
    IMPORT_UNSUPPORTED_FORMAT,
    IMPORT_EMPTY_FILE,
    IMPORT_INVALID_HEADER,
    IMPORT_ROW_LIMIT_EXCEEDED,
    IMPORT_JOB_NOT_FOUND,
    EXPORT_FAILED,
    EXPORT_INVALID_FORMAT,
    EXPORT_INVALID_DATE_RANGE,
    EXPORT_ROW_LIMIT_EXCEEDED,

    // ── AI ─────────────────────────────────────────────────────────────────
    AI_PROVIDER_UNAVAILABLE,
    AI_RATE_LIMIT_EXCEEDED,
    CONVERSATION_NOT_FOUND,

    // ── MCP ────────────────────────────────────────────────────────────────
    MCP_INVALID_API_KEY,
}
