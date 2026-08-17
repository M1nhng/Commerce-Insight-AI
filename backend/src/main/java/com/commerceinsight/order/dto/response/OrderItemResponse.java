package com.commerceinsight.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * OrderItemResponse — line item within an order detail response.
 * All name/SKU fields come from immutable snapshots, not live product data.
 */
public record OrderItemResponse(
        UUID id,
        UUID productId,
        String skuSnapshot,
        String productNameSnapshot,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal discountAmount,
        BigDecimal subtotal
) {}
