package com.commerceinsight.analytics.repository;

import com.commerceinsight.analytics.repository.projection.CustomerOrderCountRow;
import com.commerceinsight.analytics.repository.projection.PaymentMethodRow;
import com.commerceinsight.analytics.repository.projection.RevenueTimeSeriesRow;
import com.commerceinsight.analytics.repository.projection.StatusCountRow;
import com.commerceinsight.analytics.repository.projection.TopProductRow;
import com.commerceinsight.order.domain.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


/**
 * AnalyticsRepository — read-only aggregate queries for the analytics module.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>ALL queries use PostgreSQL aggregation (COUNT, SUM, GROUP BY, DATE_TRUNC).</li>
 *   <li>NO Order/Customer/Payment entities are loaded into Java memory.</li>
 *   <li>NO write operations.</li>
 *   <li>N+1 queries are structurally impossible — each method is one SQL statement.</li>
 * </ul>
 *
 * <p>Revenue-eligible statuses: CONFIRMED, PROCESSING, SHIPPED, DELIVERED, COMPLETED.
 * PENDING is excluded (payment unconfirmed). CANCELLED and REFUNDED are excluded.
 *
 * <p>Date filtering: both {@code dateFrom} and {@code dateTo} are optional (nullable).
 * Null means "no lower/upper bound" (all-time). Handled via JPQL {@code :param IS NULL OR …}.
 */
@org.springframework.stereotype.Repository
public interface AnalyticsRepository extends Repository<Order, UUID> {

    // ── Overview ──────────────────────────────────────────────────────────────

    /**
     * Total revenue from revenue-eligible orders in the period.
     * Returns 0 when no eligible orders exist.
     */
    @Query(value = """
            SELECT COALESCE(SUM(o.total), 0)
            FROM orders o
            WHERE o.status IN ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED')
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            """, nativeQuery = true)
    java.math.BigDecimal sumRevenue(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    /**
     * Total order count in the period (all statuses).
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM orders o
            WHERE (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            """, nativeQuery = true)
    long countAllOrders(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    /**
     * Count of distinct customer_ids on orders in the period (all statuses).
     * customer_id may be NULL when customer was deleted — those are excluded.
     */
    @Query(value = """
            SELECT COUNT(DISTINCT o.customer_id)
            FROM orders o
            WHERE o.customer_id IS NOT NULL
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            """, nativeQuery = true)
    long countDistinctCustomers(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    /**
     * Sum of item quantities sold across revenue-eligible orders in the period.
     * Returns 0 when no items exist.
     */
    @Query(value = """
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status IN ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED')
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            """, nativeQuery = true)
    long sumProductsSold(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    /**
     * Count of CANCELLED orders in the period.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM orders o
            WHERE o.status = 'CANCELLED'
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            """, nativeQuery = true)
    long countCancelledOrders(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    // ── Revenue time series ───────────────────────────────────────────────────

    /**
     * Revenue grouped by day (DATE_TRUNC('day', created_at)).
     * Returns only periods that have at least one revenue-eligible order.
     */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('day', o.created_at AT TIME ZONE 'UTC'), 'YYYY-MM-DD') AS period,
                   COALESCE(SUM(o.total), 0)                                                  AS revenue,
                   COUNT(*)                                                                    AS orders
            FROM orders o
            WHERE o.status IN ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED')
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY DATE_TRUNC('day', o.created_at AT TIME ZONE 'UTC')
            ORDER BY DATE_TRUNC('day', o.created_at AT TIME ZONE 'UTC')
            """, nativeQuery = true)
    List<RevenueTimeSeriesRow> revenueByDay(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    /**
     * Revenue grouped by ISO week (DATE_TRUNC('week', created_at)).
     */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('week', o.created_at AT TIME ZONE 'UTC'), 'IYYY-"W"IW') AS period,
                   COALESCE(SUM(o.total), 0)                                                    AS revenue,
                   COUNT(*)                                                                      AS orders
            FROM orders o
            WHERE o.status IN ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED')
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY DATE_TRUNC('week', o.created_at AT TIME ZONE 'UTC')
            ORDER BY DATE_TRUNC('week', o.created_at AT TIME ZONE 'UTC')
            """, nativeQuery = true)
    List<RevenueTimeSeriesRow> revenueByWeek(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    /**
     * Revenue grouped by month (DATE_TRUNC('month', created_at)).
     */
    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('month', o.created_at AT TIME ZONE 'UTC'), 'YYYY-MM') AS period,
                   COALESCE(SUM(o.total), 0)                                                 AS revenue,
                   COUNT(*)                                                                   AS orders
            FROM orders o
            WHERE o.status IN ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED')
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY DATE_TRUNC('month', o.created_at AT TIME ZONE 'UTC')
            ORDER BY DATE_TRUNC('month', o.created_at AT TIME ZONE 'UTC')
            """, nativeQuery = true)
    List<RevenueTimeSeriesRow> revenueByMonth(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    // ── Order analytics ───────────────────────────────────────────────────────

    /**
     * Returns one row per order status with the count of orders in the period.
     * Only statuses that have at least one order appear.
     */
    @Query(value = """
            SELECT o.status AS status, COUNT(*) AS orderCount
            FROM orders o
            WHERE (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY o.status
            """, nativeQuery = true)
    List<StatusCountRow> countByStatus(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    // ── Top products ──────────────────────────────────────────────────────────

    /**
     * Top N products by revenue across revenue-eligible orders in period.
     * Uses historical snapshot fields — safe when product is deleted.
     * Grouped by (product_id, sku_snapshot, product_name_snapshot) so a deleted
     * product_id (NULL) groups with its snapshots correctly.
     */
    @Query(value = """
            SELECT oi.product_id            AS productId,
                   oi.sku_snapshot          AS sku,
                   oi.product_name_snapshot AS productName,
                   SUM(oi.quantity)         AS quantitySold,
                   SUM(oi.subtotal)         AS revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            WHERE o.status IN ('CONFIRMED','PROCESSING','SHIPPED','DELIVERED','COMPLETED')
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY oi.product_id, oi.sku_snapshot, oi.product_name_snapshot
            ORDER BY SUM(oi.subtotal) DESC
            LIMIT :limitVal
            """, nativeQuery = true)
    List<TopProductRow> topProductsByRevenue(
            @Param("dateFrom")  Instant dateFrom,
            @Param("dateTo")    Instant dateTo,
            @Param("limitVal")  int limit);

    // ── Customer analytics ────────────────────────────────────────────────────

    /**
     * For each customer who placed an order in the period, returns their
     * total order count across ALL time (to distinguish new vs. returning).
     * Returns one row per customer_id.
     */
    @Query(value = """
            SELECT sub.customer_id                           AS customerId,
                   COUNT(*)                                  AS ordersInPeriod,
                   (SELECT COUNT(*) FROM orders all_o
                    WHERE all_o.customer_id = sub.customer_id) AS totalOrdersAllTime
            FROM orders sub
            WHERE sub.customer_id IS NOT NULL
              AND (CAST(:dateFrom AS timestamptz) IS NULL OR sub.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR sub.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY sub.customer_id
            """, nativeQuery = true)
    List<CustomerOrderCountRow> customerOrderCounts(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);

    // ── Payment analytics ─────────────────────────────────────────────────────

    /**
     * Payment totals grouped by method.
     * Uses the payments table directly — one payment per order.
     */
    @Query(value = """
            SELECT p.method      AS method,
                   COUNT(*)      AS orders,
                   SUM(p.amount) AS amount
            FROM payments p
            JOIN orders o ON p.order_id = o.id
            WHERE (CAST(:dateFrom AS timestamptz) IS NULL OR o.created_at >= CAST(:dateFrom AS timestamptz))
              AND (CAST(:dateTo AS timestamptz)   IS NULL OR o.created_at <= CAST(:dateTo AS timestamptz))
            GROUP BY p.method
            ORDER BY SUM(p.amount) DESC
            """, nativeQuery = true)
    List<PaymentMethodRow> paymentBreakdown(
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo")   Instant dateTo);
}
