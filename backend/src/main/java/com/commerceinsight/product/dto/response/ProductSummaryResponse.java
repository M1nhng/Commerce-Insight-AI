package com.commerceinsight.product.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ProductSummaryResponse — lightweight DTO for paginated product list.
 *
 * <p>Per API spec §9 — GET /api/v1/products returns this type.
 * Does not include description or cost_price to reduce payload size.
 *
 * <p>stockQuantity is a placeholder (0) in Sprint 6.
 * Sprint 7 (Inventory) will join the inventory table and populate it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductSummaryResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        UUID categoryId,
        String categoryName,
        int stockQuantity,
        boolean active,
        String imageUrl,
        Instant createdAt
) {}
