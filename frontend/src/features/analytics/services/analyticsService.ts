/**
 * features/analytics/services/analyticsService.ts
 *
 * API calls for the analytics domain — mirrors all AnalyticsController endpoints.
 *
 * Rules:
 * - Uses the existing apiClient (no second HTTP client).
 * - All params are passed as query strings.
 * - Returns ApiResponse<T> data via .then(r => r.data).
 * - null date params are omitted from the request (undefined = omit).
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse } from '@/types/api.types'
import type {
  OverviewResponse,
  RevenueResponse,
  OrderAnalyticsResponse,
  TopProductEntry,
  CustomerAnalyticsResponse,
  PaymentAnalyticsResponse,
  AnalyticsDateRange,
  RevenueGroupBy,
} from '../types/analytics.types'

const BASE = '/analytics'

/** Strips null/undefined values so Axios doesn't send them as "null" strings. */
function cleanParams(params: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== null && v !== undefined)
  )
}

export const analyticsService = {
  /**
   * GET /api/v1/analytics/overview
   * Returns high-level KPI snapshot.
   */
  getOverview(range: AnalyticsDateRange) {
    return apiClient
      .get<ApiResponse<OverviewResponse>>(`${BASE}/overview`, {
        params: cleanParams({ dateFrom: range.dateFrom, dateTo: range.dateTo }),
      })
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/analytics/revenue
   * Returns revenue time series grouped by DAY | WEEK | MONTH.
   */
  getRevenue(range: AnalyticsDateRange, groupBy: RevenueGroupBy = 'DAY') {
    return apiClient
      .get<ApiResponse<RevenueResponse>>(`${BASE}/revenue`, {
        params: cleanParams({ dateFrom: range.dateFrom, dateTo: range.dateTo, groupBy }),
      })
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/analytics/orders
   * Returns order status breakdown + completion/cancellation rates.
   */
  getOrders(range: AnalyticsDateRange) {
    return apiClient
      .get<ApiResponse<OrderAnalyticsResponse>>(`${BASE}/orders`, {
        params: cleanParams({ dateFrom: range.dateFrom, dateTo: range.dateTo }),
      })
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/analytics/products/top
   * Returns top N products by revenue.
   */
  getTopProducts(range: AnalyticsDateRange, limit = 10) {
    return apiClient
      .get<ApiResponse<TopProductEntry[]>>(`${BASE}/products/top`, {
        params: cleanParams({ dateFrom: range.dateFrom, dateTo: range.dateTo, limit }),
      })
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/analytics/customers
   * Returns customer engagement metrics.
   */
  getCustomers(range: AnalyticsDateRange) {
    return apiClient
      .get<ApiResponse<CustomerAnalyticsResponse>>(`${BASE}/customers`, {
        params: cleanParams({ dateFrom: range.dateFrom, dateTo: range.dateTo }),
      })
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/analytics/payments
   * Returns payment method breakdown.
   */
  getPayments(range: AnalyticsDateRange) {
    return apiClient
      .get<ApiResponse<PaymentAnalyticsResponse>>(`${BASE}/payments`, {
        params: cleanParams({ dateFrom: range.dateFrom, dateTo: range.dateTo }),
      })
      .then((r) => r.data)
  },
}
