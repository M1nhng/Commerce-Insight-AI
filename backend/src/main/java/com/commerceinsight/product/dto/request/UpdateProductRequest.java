package com.commerceinsight.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * UpdateProductRequest — validated request body for PUT /api/v1/products/{id}.
 * Full replacement update — all mutable fields must be supplied.
 */
public record UpdateProductRequest(

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
        @Digits(integer = 15, fraction = 4, message = "Price format is invalid")
        BigDecimal price,

        @DecimalMin(value = "0.0", inclusive = true, message = "Cost price must be 0 or greater")
        @Digits(integer = 15, fraction = 4, message = "Cost price format is invalid")
        BigDecimal costPrice,

        @Size(max = 1000, message = "Image URL must not exceed 1000 characters")
        String imageUrl,

        UUID categoryId,

        Boolean active
) {
    public boolean resolvedActive() {
        return active == null || active;
    }
}
