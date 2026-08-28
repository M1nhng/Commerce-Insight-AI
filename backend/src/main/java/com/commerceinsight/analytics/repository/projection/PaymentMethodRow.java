package com.commerceinsight.analytics.repository.projection;

import java.math.BigDecimal;

/**
 * Projection for payment-method breakdown native query.
 * Maps: method (String), orders (long), amount (BigDecimal).
 */
public interface PaymentMethodRow {
    String getMethod();
    long getOrders();
    BigDecimal getAmount();
}
