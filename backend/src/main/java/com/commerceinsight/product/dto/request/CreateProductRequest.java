package com.commerceinsight.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CreateProductRequest — validated request body for POST /api/v1/products.
 */
public record CreateProductRequest(

        @NotBlank(message = "SKU must not be blank")
        @Size(max = 100, message = "SKU must not exceed 100 characters")
        String sku,

        @NotBlank(message = "Product name must not be blank")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be 0 or greater")
        @Digits(integer = 15, fraction = 4, message = "Price must have at most 15 integer and 4 decimal digits")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Cost price must be 0 or greater")
        @Digits(integer = 15, fraction = 4, message = "Cost price must have at most 15 integer and 4 decimal digits")
        BigDecimal costPrice,

        @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
        String imageUrl,

        /** Nullable — product may not be assigned to a category. */
        UUID categoryId,

        /**
         * Initial stock quantity to seed the inventory record.
         * 0 means no stock yet. Null defaults to 0.
         * NOTE: Inventory seeding is handled by Inventory module (Sprint 7).
         */
        @Min(value = 0, message = "Initial stock must be 0 or greater")
        Integer initialStock
) {
    public int resolvedInitialStock() {
        return initialStock != null ? initialStock : 0;
    }
}
