package com.commerceinsight.dataimport.domain;

/**
 * ImportJobStatus — lifecycle states for an import job.
 *
 * <p>Transitions:
 * <pre>
 * UPLOADED → VALIDATING → IMPORTING → COMPLETED
 *                                   → PARTIAL_SUCCESS
 *                                   → FAILED
 *          → FAILED (on file validation failure)
 * </pre>
 */
public enum ImportJobStatus {

    /** File received and job record created; processing not yet started. */
    UPLOADED,

    /** File structure and headers are being validated. */
    VALIDATING,

    /** Rows are being imported one by one into domain services. */
    IMPORTING,

    /** All rows were imported successfully (failedRows == 0). */
    COMPLETED,

    /** Import finished but some rows failed (failedRows > 0 && successfulRows > 0). */
    PARTIAL_SUCCESS,

    /** Import failed entirely — either file validation failed or all rows failed. */
    FAILED
}
