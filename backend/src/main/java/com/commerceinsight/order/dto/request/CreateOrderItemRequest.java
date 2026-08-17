package com.commerceinsight.order.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CreateOrderItemRequest — a single line item for order creation.
 *
 * <p>discountAmount is an optional per-item discount. Backend calculates subtotal.
 * unitPrice comes from the product at creation time — validated server-side.
 */
public record CreateOrderItemRequest(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10_000, message = "Quantity must not exceed 10,000")
        Integer quantity,

        /** Optional per-item discount. If null, defaults to 0. */
        @DecimalMin(value = "0.0", inclusive = true, message = "Discount amount cannot be negative")
        BigDecimal discountAmount
) {}
