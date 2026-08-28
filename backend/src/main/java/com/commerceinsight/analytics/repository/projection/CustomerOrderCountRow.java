package com.commerceinsight.analytics.repository.projection;

import java.util.UUID;

/**
 * Projection for customer order-count native query.
 * Maps: customerId, ordersInPeriod, totalOrdersAllTime.
 */
public interface CustomerOrderCountRow {
    UUID getCustomerId();
    long getOrdersInPeriod();
    long getTotalOrdersAllTime();
}
