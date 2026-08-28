/**
 * features/analytics/hooks/useAnalytics.ts
 *
 * TanStack Query hooks for the analytics domain.
 * All queries automatically refetch when date range or groupBy changes
 * (keys include the full params object).
 */
import { useQuery } from '@tanstack/react-query'
import { analyticsService } from '../services/analyticsService'
import type { AnalyticsDateRange, RevenueGroupBy } from '../types/analytics.types'

// ── Query key factory ─────────────────────────────────────────────────────

export const ANALYTICS_KEYS = {
  all: ['analytics'] as const,
  overview:  (range: AnalyticsDateRange) =>
    [...ANALYTICS_KEYS.all, 'overview', range] as const,
  revenue:   (range: AnalyticsDateRange, groupBy: RevenueGroupBy) =>
    [...ANALYTICS_KEYS.all, 'revenue', range, groupBy] as const,
  orders:    (range: AnalyticsDateRange) =>
    [...ANALYTICS_KEYS.all, 'orders', range] as const,
  products:  (range: AnalyticsDateRange, limit: number) =>
    [...ANALYTICS_KEYS.all, 'products', range, limit] as const,
  customers: (range: AnalyticsDateRange) =>
    [...ANALYTICS_KEYS.all, 'customers', range] as const,
  payments:  (range: AnalyticsDateRange) =>
    [...ANALYTICS_KEYS.all, 'payments', range] as const,
}

// Stale time: analytics data is relatively stable — 2 minutes is appropriate.
const STALE_TIME = 2 * 60 * 1000

// ── Hooks ─────────────────────────────────────────────────────────────────

export function useAnalyticsOverview(range: AnalyticsDateRange) {
  return useQuery({
    queryKey: ANALYTICS_KEYS.overview(range),
    queryFn:  () => analyticsService.getOverview(range),
    select:   (data) => data.data,
    staleTime: STALE_TIME,
  })
}

export function useRevenueAnalytics(range: AnalyticsDateRange, groupBy: RevenueGroupBy = 'DAY') {
  return useQuery({
    queryKey: ANALYTICS_KEYS.revenue(range, groupBy),
    queryFn:  () => analyticsService.getRevenue(range, groupBy),
    select:   (data) => data.data,
    staleTime: STALE_TIME,
  })
}

export function useOrderAnalytics(range: AnalyticsDateRange) {
  return useQuery({
    queryKey: ANALYTICS_KEYS.orders(range),
    queryFn:  () => analyticsService.getOrders(range),
    select:   (data) => data.data,
    staleTime: STALE_TIME,
  })
}

export function useTopProducts(range: AnalyticsDateRange, limit = 10) {
  return useQuery({
    queryKey: ANALYTICS_KEYS.products(range, limit),
    queryFn:  () => analyticsService.getTopProducts(range, limit),
    select:   (data) => data.data ?? [],
    staleTime: STALE_TIME,
  })
}

export function useCustomerAnalytics(range: AnalyticsDateRange) {
  return useQuery({
    queryKey: ANALYTICS_KEYS.customers(range),
    queryFn:  () => analyticsService.getCustomers(range),
    select:   (data) => data.data,
    staleTime: STALE_TIME,
  })
}

export function usePaymentAnalytics(range: AnalyticsDateRange) {
  return useQuery({
    queryKey: ANALYTICS_KEYS.payments(range),
    queryFn:  () => analyticsService.getPayments(range),
    select:   (data) => data.data,
    staleTime: STALE_TIME,
  })
}
