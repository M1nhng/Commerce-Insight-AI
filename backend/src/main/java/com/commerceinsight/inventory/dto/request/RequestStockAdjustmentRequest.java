package com.commerceinsight.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * RequestStockAdjustmentRequest — request body for POST /api/v1/stock-adjustments.
 *
 * <p>Creates a PENDING adjustment that must be approved by an ADMIN before
 * it affects inventory. MANAGER and STAFF can create requests.
 */
public record RequestStockAdjustmentRequest(

        @NotNull(message = "Inventory ID is required")
        UUID inventoryId,

        @NotNull(message = "Quantity delta is required")
        int quantityDelta,  // positive = add, negative = remove

        @NotBlank(message = "Reason is required")
        @Size(max = 2000, message = "Reason must not exceed 2000 characters")
        String reason
) {}
