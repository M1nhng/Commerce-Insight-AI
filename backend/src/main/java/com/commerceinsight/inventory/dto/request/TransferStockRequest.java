package com.commerceinsight.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * TransferStockRequest — request body for POST /api/v1/inventory/transfer.
 *
 * <p>Transfers a quantity of stock from one warehouse to another in a single
 * atomic transaction. Creates one TRANSFER_OUT and one TRANSFER_IN record.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>Source and destination warehouses must be different.</li>
 *   <li>Source must have sufficient available stock (quantity - reserved >= transferQuantity).</li>
 *   <li>Destination inventory row is created if it does not exist.</li>
 * </ul>
 */
public record TransferStockRequest(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Source warehouse ID is required")
        UUID sourceWarehouseId,

        @NotNull(message = "Destination warehouse ID is required")
        UUID destinationWarehouseId,

        @NotNull(message = "Transfer quantity is required")
        @Positive(message = "Transfer quantity must be positive")
        int quantity,

        String notes
) {}
