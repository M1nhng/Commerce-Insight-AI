package com.commerceinsight.analytics.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projection for top-products native query.
 * Maps: productId (UUID, nullable), sku, productName, quantitySold, revenue.
 */
public interface TopProductRow {
    UUID getProductId();
    String getSku();
    String getProductName();
    long getQuantitySold();
    BigDecimal getRevenue();
}
