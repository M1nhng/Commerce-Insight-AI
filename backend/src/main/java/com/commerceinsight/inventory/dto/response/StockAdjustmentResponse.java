package com.commerceinsight.inventory.dto.response;

import com.commerceinsight.inventory.domain.AdjustmentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * StockAdjustmentResponse — response DTO for stock adjustment endpoints.
 */
public record StockAdjustmentResponse(
        UUID id,
        UUID inventoryId,
        UUID productId,
        String productName,
        String productSku,
        UUID warehouseId,
        String warehouseName,
        int quantityDelta,
        String reason,
        AdjustmentStatus status,
        UUID requestedById,
        String requestedByName,
        UUID reviewedById,
        String reviewedByName,
        Instant reviewedAt,
        String reviewNotes,
        UUID transactionId,
        Instant createdAt,
        Instant updatedAt
) {}
