/**
 * features/analytics/utils/dateUtils.ts
 *
 * Date range utilities for the analytics dashboard.
 *
 * Rules:
 * - All dates sent to the backend must be ISO 8601 UTC Instants.
 * - The UI displays dates in local Vietnamese timezone (Asia/Ho_Chi_Minh, UTC+7).
 * - date-fns is used for reliable date arithmetic.
 */
import {
  startOfDay,
  endOfDay,
  startOfMonth,
  endOfMonth,
  subDays,
  subMonths,
  format,
} from 'date-fns'

export type DatePreset =
  | 'today'
  | 'yesterday'
  | 'last7days'
  | 'last30days'
  | 'thisMonth'
  | 'lastMonth'
  | 'allTime'
  | 'custom'

export interface DateRangeResult {
  dateFrom: string | null  // ISO 8601 UTC e.g. "2026-08-01T00:00:00.000Z"
  dateTo:   string | null
  label:    string
}

/**
 * Converts a local Date to a UTC ISO 8601 instant string.
 * The backend treats all timestamps as UTC.
 */
export function toISOInstant(d: Date): string {
  return d.toISOString()
}

/**
 * Computes dateFrom/dateTo for a given preset.
 * Returns null for both when preset is 'allTime' or 'custom'.
 */
export function getDateRange(preset: DatePreset): DateRangeResult {
  const now = new Date()

  switch (preset) {
    case 'today':
      return {
        dateFrom: toISOInstant(startOfDay(now)),
        dateTo:   toISOInstant(endOfDay(now)),
        label:    'Today',
      }
    case 'yesterday': {
      const yesterday = subDays(now, 1)
      return {
        dateFrom: toISOInstant(startOfDay(yesterday)),
        dateTo:   toISOInstant(endOfDay(yesterday)),
        label:    'Yesterday',
      }
    }
    case 'last7days':
      return {
        dateFrom: toISOInstant(startOfDay(subDays(now, 6))),
        dateTo:   toISOInstant(endOfDay(now)),
        label:    'Last 7 Days',
      }
    case 'last30days':
      return {
        dateFrom: toISOInstant(startOfDay(subDays(now, 29))),
        dateTo:   toISOInstant(endOfDay(now)),
        label:    'Last 30 Days',
      }
    case 'thisMonth':
      return {
        dateFrom: toISOInstant(startOfMonth(now)),
        dateTo:   toISOInstant(endOfDay(now)),
        label:    'This Month',
      }
    case 'lastMonth': {
      const lastMonth = subMonths(now, 1)
      return {
        dateFrom: toISOInstant(startOfMonth(lastMonth)),
        dateTo:   toISOInstant(endOfMonth(lastMonth)),
        label:    'Last Month',
      }
    }
    case 'allTime':
    case 'custom':
    default:
      return { dateFrom: null, dateTo: null, label: 'All Time' }
  }
}

/**
 * Formats an ISO 8601 string for display in the UI.
 * Returns empty string for null.
 */
export function formatDisplayDate(iso: string | null): string {
  if (!iso) return ''
  try {
    return format(new Date(iso), 'dd/MM/yyyy')
  } catch {
    return ''
  }
}

/**
 * Formats a number as Vietnamese Dong (₫).
 */
export function formatVND(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount)
}

/**
 * Formats a currency amount using the provided currency code.
 * Falls back to VND if currency is VND or unrecognized.
 */
export function formatCurrency(amount: number, currency: string): string {
  if (currency === 'VND') return formatVND(amount)
  try {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency,
      minimumFractionDigits: 0,
    }).format(amount)
  } catch {
    return formatVND(amount)
  }
}

/**
 * Formats a number as a percentage with 2 decimal places.
 */
export function formatPercent(value: number): string {
  return `${value.toFixed(2)}%`
}

/**
 * Formats a large integer with locale separators.
 */
export function formatNumber(value: number): string {
  return new Intl.NumberFormat('vi-VN').format(value)
}

/**
 * Human-readable labels for payment method enum values.
 */
export const PAYMENT_METHOD_LABELS: Record<string, string> = {
  CASH:          'Cash',
  BANK_TRANSFER: 'Bank Transfer',
  CARD:          'Card',
  OTHER:         'Other',
}
