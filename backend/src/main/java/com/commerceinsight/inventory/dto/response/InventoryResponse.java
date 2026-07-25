package com.commerceinsight.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * InventoryResponse — response DTO for inventory endpoints.
 *
 * <p>Includes product and warehouse details (denormalized) for easy UI rendering.
 */
public record InventoryResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSku,
        UUID warehouseId,
        String warehouseName,
        String warehouseCode,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        int lowStockThreshold,
        boolean lowStock,
        Instant updatedAt
) {}
