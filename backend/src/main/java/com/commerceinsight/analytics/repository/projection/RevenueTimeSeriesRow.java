package com.commerceinsight.analytics.repository.projection;

import java.math.BigDecimal;

/**
 * Projection for revenue time-series native queries.
 * Maps: period (String), revenue (BigDecimal), orders (long).
 */
public interface RevenueTimeSeriesRow {
    String getPeriod();
    BigDecimal getRevenue();
    long getOrders();
}
