package com.commerceinsight.analytics.repository.projection;

/**
 * Projection for order count-by-status native query.
 * Maps: status (String), orderCount (long).
 */
public interface StatusCountRow {
    String getStatus();
    long getOrderCount();
}
