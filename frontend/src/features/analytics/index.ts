/**
 * features/analytics/index.ts — Public API
 */
export { AnalyticsPage } from './pages/AnalyticsPage'

// Types
export type {
  OverviewResponse,
  RevenuePeriodResponse,
  RevenueResponse,
  OrderAnalyticsResponse,
  TopProductEntry,
  CustomerAnalyticsResponse,
  PaymentMethodStats,
  PaymentAnalyticsResponse,
  AnalyticsDateRange,
  RevenueGroupBy,
  AnalyticsParams,
} from './types/analytics.types'

// Hooks
export {
  ANALYTICS_KEYS,
  useAnalyticsOverview,
  useRevenueAnalytics,
  useOrderAnalytics,
  useTopProducts,
  useCustomerAnalytics,
  usePaymentAnalytics,
} from './hooks/useAnalytics'

// Service
export { analyticsService } from './services/analyticsService'
