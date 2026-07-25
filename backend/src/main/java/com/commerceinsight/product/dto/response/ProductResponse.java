package com.commerceinsight.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ProductResponse — full product detail DTO.
 *
 * <p>Returned by GET /api/v1/products/{id}, POST /api/v1/products, PUT /api/v1/products/{id}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        BigDecimal costPrice,
        String imageUrl,
        UUID categoryId,
        String categoryName,
        boolean active,
        int stockQuantity,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
    /** Embedded image DTO. */
    public record ProductImageResponse(
            UUID id,
            String url,
            String altText,
            int sortOrder
    ) {}
}
