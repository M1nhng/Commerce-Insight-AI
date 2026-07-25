package com.commerceinsight.inventory.dto.request;

import jakarta.validation.constraints.Size;

/**
 * ReviewStockAdjustmentRequest — request body for
 * PATCH /api/v1/stock-adjustments/{id}/approve and /reject.
 *
 * <p>ADMIN only. Provides optional reviewer notes for both decisions.
 */
public record ReviewStockAdjustmentRequest(

        @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
        String reviewNotes
) {}
