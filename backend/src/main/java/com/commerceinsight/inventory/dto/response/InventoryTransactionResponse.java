package com.commerceinsight.inventory.dto.response;

import com.commerceinsight.inventory.domain.TransactionType;

import java.time.Instant;
import java.util.UUID;

/**
 * InventoryTransactionResponse — response DTO for transaction history endpoints.
 */
public record InventoryTransactionResponse(
        UUID id,
        UUID inventoryId,
        UUID productId,
        String productName,
        String productSku,
        UUID warehouseId,
        String warehouseName,
        UUID performedById,
        String performedByName,
        TransactionType type,
        int quantity,
        int quantityBefore,
        int quantityAfter,
        UUID referenceId,
        String notes,
        Instant createdAt
) {}
