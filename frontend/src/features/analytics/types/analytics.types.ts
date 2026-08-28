/**
 * features/analytics/types/analytics.types.ts
 *
 * Type definitions mirroring the Spring Boot Analytics DTOs exactly.
 * Field names must match the backend JSON — verified against:
 *   - OverviewResponse.java
 *   - RevenuePeriodResponse.java
 *   - RevenueResponse.java
 *   - OrderAnalyticsResponse.java
 *   - TopProductEntry.java
 *   - CustomerAnalyticsResponse.java
 *   - PaymentMethodStats.java
 *   - PaymentAnalyticsResponse.java
 */

// ── Enums ─────────────────────────────────────────────────────────────────

export type RevenueGroupBy = 'DAY' | 'WEEK' | 'MONTH'

// ── Date range helper ─────────────────────────────────────────────────────

export interface AnalyticsDateRange {
  dateFrom: string | null  // ISO 8601 UTC instant: "2026-08-01T00:00:00Z"
  dateTo:   string | null  // ISO 8601 UTC instant: "2026-08-31T23:59:59Z"
}

// ── Backend DTOs (field names must match JSON exactly) ────────────────────

/**
 * Mirrors OverviewResponse — high-level KPI snapshot.
 */
export interface OverviewResponse {
  totalRevenue:       number
  totalOrders:        number
  totalCustomers:     number
  totalProductsSold:  number
  averageOrderValue:  number
  cancelledOrders:    number
  cancellationRate:   number
  currency:           string
  dateFrom:           string | null
  dateTo:             string | null
}

/**
 * Mirrors RevenuePeriodResponse — one data point in a revenue time series.
 */
export interface RevenuePeriodResponse {
  period:  string   // "2026-08-01" | "2026-W32" | "2026-08"
  revenue: number
  orders:  number
}

/**
 * Mirrors RevenueResponse — time series with metadata.
 */
export interface RevenueResponse {
  groupBy:  string
  currency: string
  dateFrom: string | null
  dateTo:   string | null
  data:     RevenuePeriodResponse[]
}

/**
 * Mirrors OrderAnalyticsResponse — per-status counts with rates.
 */
export interface OrderAnalyticsResponse {
  totalOrders:       number
  pendingOrders:     number
  confirmedOrders:   number
  processingOrders:  number
  shippedOrders:     number
  deliveredOrders:   number
  completedOrders:   number
  cancelledOrders:   number
  completionRate:    number
  cancellationRate:  number
  dateFrom:          string | null
  dateTo:            string | null
}

/**
 * Mirrors TopProductEntry — one row in the top products leaderboard.
 * productId is nullable (product may have been deleted).
 */
export interface TopProductEntry {
  productId:    string | null
  sku:          string
  productName:  string
  quantitySold: number
  revenue:      number
}

/**
 * Mirrors CustomerAnalyticsResponse — customer engagement metrics.
 */
export interface CustomerAnalyticsResponse {
  uniqueCustomers:         number
  newCustomers:            number
  repeatCustomers:         number
  averageOrdersPerCustomer: number
  dateFrom:                string | null
  dateTo:                  string | null
}

/**
 * Mirrors PaymentMethodStats — aggregated stats for one payment method.
 */
export interface PaymentMethodStats {
  orders: number
  amount: number
}

/**
 * Mirrors PaymentAnalyticsResponse — payment breakdown keyed by method.
 * Map keys: "CASH" | "BANK_TRANSFER" | "CARD" | "OTHER"
 */
export interface PaymentAnalyticsResponse {
  currency:  string
  breakdown: Record<string, PaymentMethodStats>
  dateFrom:  string | null
  dateTo:    string | null
}

// ── Query params ──────────────────────────────────────────────────────────

export interface AnalyticsParams extends AnalyticsDateRange {
  groupBy?: RevenueGroupBy
  limit?:   number
}
