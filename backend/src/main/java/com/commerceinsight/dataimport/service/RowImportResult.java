package com.commerceinsight.dataimport.service;

import java.util.Collections;
import java.util.List;

/**
 * RowImportResult — the outcome of attempting to import a single data row.
 *
 * <p>Returned by all domain import services (Product/Customer/Order)
 * so that {@link ImportOrchestrator} can update counters and persist errors
 * without coupling to domain-specific exception types.
 */
public record RowImportResult(
        int rowNumber,
        boolean success,
        List<RowError> errors
) {

    public static RowImportResult success(int rowNumber) {
        return new RowImportResult(rowNumber, true, Collections.emptyList());
    }

    public static RowImportResult failure(int rowNumber, List<RowError> errors) {
        return new RowImportResult(rowNumber, false, errors == null ? Collections.emptyList() : errors);
    }
}
