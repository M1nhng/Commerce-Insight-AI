/**
 * features/export/constants.ts
 *
 * Static catalog of exportable reports (Sprint 11B). Pure data — no JSX.
 */
import type { ExportReportMeta, ExportReportType } from './types/export.types'

export const EXPORT_REPORTS: ExportReportMeta[] = [
  // ── Catalog ────────────────────────────────────────────────────────────
  {
    type: 'PRODUCTS',
    label: 'Products',
    description: 'Export your product catalog, pricing and status.',
    category: 'CATALOG',
    fallbackBase: 'products',
  },
  {
    type: 'CUSTOMERS',
    label: 'Customers',
    description: 'Export customer records with contact and group details.',
    category: 'CATALOG',
    fallbackBase: 'customers',
  },
  {
    type: 'ORDERS',
    label: 'Orders',
    description: 'Export order records — one row per order — with totals.',
    category: 'CATALOG',
    fallbackBase: 'orders',
  },
  // ── Analytics ──────────────────────────────────────────────────────────
  {
    type: 'REVENUE',
    label: 'Revenue',
    description: 'Revenue trend grouped by day, week or month.',
    category: 'ANALYTICS',
    fallbackBase: 'revenue-analytics',
  },
  {
    type: 'ORDER_ANALYTICS',
    label: 'Order Analytics',
    description: 'Order counts by status with completion and cancellation rates.',
    category: 'ANALYTICS',
    fallbackBase: 'order-analytics',
  },
  {
    type: 'TOP_PRODUCTS',
    label: 'Top Products',
    description: 'Best-selling products ranked by revenue.',
    category: 'ANALYTICS',
    fallbackBase: 'top-products-analytics',
  },
  {
    type: 'CUSTOMER_ANALYTICS',
    label: 'Customer Analytics',
    description: 'Customer engagement: new, repeat and unique buyers.',
    category: 'ANALYTICS',
    fallbackBase: 'customer-analytics',
  },
  {
    type: 'PAYMENT_ANALYTICS',
    label: 'Payment Analytics',
    description: 'Order value and count broken down by payment method.',
    category: 'ANALYTICS',
    fallbackBase: 'payment-analytics',
  },
]

export const EXPORT_REPORT_MAP: Record<ExportReportType, ExportReportMeta> =
  EXPORT_REPORTS.reduce(
    (acc, r) => {
      acc[r.type] = r
      return acc
    },
    {} as Record<ExportReportType, ExportReportMeta>,
  )

export const TOP_PRODUCTS_LIMIT_MIN = 1
export const TOP_PRODUCTS_LIMIT_MAX = 100
export const TOP_PRODUCTS_LIMIT_DEFAULT = 10