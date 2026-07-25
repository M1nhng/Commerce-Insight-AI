package com.commerceinsight.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * AdjustStockRequest — request body for PATCH /api/v1/inventory/{id}/adjust.
 *
 * <p>Performs an immediate, admin-only stock adjustment without the
 * approval workflow. For workflow-based adjustments, use
 * {@link RequestStockAdjustmentRequest}.
 */
public record AdjustStockRequest(

        @NotNull(message = "Quantity is required")
        int quantity,  // positive = add, negative = remove

        String notes,

        @Min(value = 0, message = "Low stock threshold must be non-negative")
        Integer lowStockThreshold  // optional — also update the threshold
) {}
