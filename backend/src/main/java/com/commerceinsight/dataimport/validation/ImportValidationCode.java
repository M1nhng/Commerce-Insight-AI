package com.commerceinsight.dataimport.validation;

/**
 * ImportValidationCode — stable string error codes for per-row import errors.
 *
 * <p>These codes are stored in import_errors.error_code and returned via the API.
 * Frontend and MCP tools can use these for programmatic handling.
 *
 * <p>Naming convention: SCREAMING_SNAKE_CASE.
 */
public final class ImportValidationCode {

    private ImportValidationCode() {}

    // ── File-level errors (not per-row) ──────────────────────────────────────
    public static final String FILE_TOO_LARGE        = "FILE_TOO_LARGE";
    public static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";
    public static final String EMPTY_FILE            = "EMPTY_FILE";
    public static final String INVALID_HEADER        = "INVALID_HEADER";
    public static final String ROW_LIMIT_EXCEEDED    = "ROW_LIMIT_EXCEEDED";

    // ── Row-level validation errors ───────────────────────────────────────────
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    public static final String INVALID_VALUE          = "INVALID_VALUE";
    public static final String INVALID_FORMAT         = "INVALID_FORMAT";
    public static final String VALUE_TOO_LONG         = "VALUE_TOO_LONG";
    public static final String NEGATIVE_VALUE         = "NEGATIVE_VALUE";

    // ── Business rule errors ──────────────────────────────────────────────────
    public static final String DUPLICATE_RECORD       = "DUPLICATE_RECORD";
    public static final String ENTITY_NOT_FOUND       = "ENTITY_NOT_FOUND";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String INVENTORY_UNAVAILABLE  = "INVENTORY_UNAVAILABLE";
    public static final String CUSTOMER_INACTIVE      = "CUSTOMER_INACTIVE";
    public static final String PRODUCT_INACTIVE       = "PRODUCT_INACTIVE";
}
