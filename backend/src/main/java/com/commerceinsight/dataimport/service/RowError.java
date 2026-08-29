package com.commerceinsight.dataimport.service;

/**
 * RowError — details of a single field-level failure within an import row.
 *
 * <p>Used by domain import services to communicate validation and business rule
 * failures back to {@link ImportOrchestrator} for persistence via {@link ImportJobService}.
 *
 * @param fieldName    the column name that caused the error (null for row-level errors)
 * @param rawValue     the raw cell value that caused the error (may be null)
 * @param errorCode    stable code from {@link com.commerceinsight.dataimport.validation.ImportValidationCode}
 * @param errorMessage human-readable description
 */
public record RowError(
        String fieldName,
        String rawValue,
        String errorCode,
        String errorMessage
) {}
