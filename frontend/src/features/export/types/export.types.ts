/**
 * features/export/types/export.types.ts
 *
 * Frontend-state types for the Export Center (Sprint 11B).
 *
 * These describe the UI state and the query parameters we send to the
 * Sprint 11A backend export endpoints (GET /api/v1/export/**). They are NOT a
 * mirror of any backend DTO — the export endpoints return binary files, not JSON.
 *
 * Do not confuse ExportReportType with the import feature's ImportType.
 */
import type { CustomerStatus } from '@/types/customer.types'
import type { OrderStatus, PaymentStatus } from '@/types/order.types'

// ── Format ────────────────────────────────────────────────────────────────

export type ExportFormat = 'PDF' | 'XLSX'

/** Sent to the backend `format` query param (case-insensitive on the server). */
export const EXPORT_FORMAT_PARAM: Record<ExportFormat, string> = {
  PDF: 'pdf',
  XLSX: 'xlsx',
}

// ── Report types ──────────────────────────────────────────────────────────

export type ExportReportType =
  | 'PRODUCTS'
  | 'CUSTOMERS'
  | 'ORDERS'
  | 'REVENUE'
  | 'ORDER_ANALYTICS'
  | 'TOP_PRODUCTS'
  | 'CUSTOMER_ANALYTICS'
  | 'PAYMENT_ANALYTICS'

export type ExportReportCategory = 'CATALOG' | 'ANALYTICS'

/** Local copy — the backend accepts DAY | WEEK | MONTH for revenue grouping. */
export type RevenueGroupBy = 'DAY' | 'WEEK' | 'MONTH'

// ── Per-report parameter shapes (only supported fields) ───────────────────

export interface ProductExportParams {
  search?: string
  categoryId?: string
  active?: boolean
  priceMin?: number
  priceMax?: number
}

export interface CustomerExportParams {
  keyword?: string
  status?: CustomerStatus
  groupId?: string
  /** ISO-8601 instant, e.g. 2026-08-01T00:00:00.000Z */
  startDate?: string
  /** ISO-8601 instant */
  endDate?: string
}

export interface OrderExportParams {
  keyword?: string
  customerId?: string
  status?: OrderStatus
  paymentStatus?: PaymentStatus
  /** ISO-8601 instant */
  dateFrom?: string
  /** ISO-8601 instant */
  dateTo?: string
}

export interface DateRangeExportParams {
  /** ISO-8601 instant */
  dateFrom?: string
  /** ISO-8601 instant */
  dateTo?: string
}

export interface RevenueExportParams extends DateRangeExportParams {
  groupBy?: RevenueGroupBy
}

export interface TopProductsExportParams extends DateRangeExportParams {
  /** 1–100, backend default 10 */
  limit?: number
}

/** order / customer / payment analytics all take only a date range. */
export type AnalyticsExportParams = DateRangeExportParams

// ── Discriminated request union consumed by useExportReport() ─────────────

export type ExportRequest =
  | { type: 'PRODUCTS'; format: ExportFormat; params: ProductExportParams }
  | { type: 'CUSTOMERS'; format: ExportFormat; params: CustomerExportParams }
  | { type: 'ORDERS'; format: ExportFormat; params: OrderExportParams }
  | { type: 'REVENUE'; format: ExportFormat; params: RevenueExportParams }
  | { type: 'ORDER_ANALYTICS'; format: ExportFormat; params: AnalyticsExportParams }
  | { type: 'TOP_PRODUCTS'; format: ExportFormat; params: TopProductsExportParams }
  | { type: 'CUSTOMER_ANALYTICS'; format: ExportFormat; params: AnalyticsExportParams }
  | { type: 'PAYMENT_ANALYTICS'; format: ExportFormat; params: AnalyticsExportParams }

/** Union of every per-report param object — handy for generic filter state. */
export type AnyExportParams =
  | ProductExportParams
  | CustomerExportParams
  | OrderExportParams
  | RevenueExportParams
  | TopProductsExportParams
  | AnalyticsExportParams

// ── Report catalog metadata ──────────────────────────────────────────────

export interface ExportReportMeta {
  type: ExportReportType
  label: string
  description: string
  category: ExportReportCategory
  /** kebab fallback base filename, e.g. "revenue-analytics" */
  fallbackBase: string
}

// ── Download result ──────────────────────────────────────────────────────

export interface ExportDownloadResult {
  filename: string
  format: ExportFormat
  sizeBytes: number
}

// ── Raw filter-form state (UI only) ──────────────────────────────────────
//
// One flat, string-based form object shared by every report's filter panel.
// Values are converted to typed, cleaned request params only at submit time
// (see pages/ExportPage.tsx#buildRequest). Switching report type resets this
// to EMPTY_EXPORT_FORM, so a param from one report can never leak into another.

export interface ExportFormState {
  /** Free-text search — mapped to `search` (products) or `keyword` (customers/orders). */
  keyword: string
  categoryId: string
  /** '' = any, 'true' / 'false' = explicit. */
  active: string
  priceMin: string
  priceMax: string
  customerStatus: string
  groupId: string
  customerId: string
  orderStatus: string
  paymentStatus: string
  /** yyyy-mm-dd from <input type="date">. */
  dateFrom: string
  /** yyyy-mm-dd from <input type="date">. */
  dateTo: string
  groupBy: RevenueGroupBy
  /** kept as a string for the number input; validated to 1–100. */
  limit: string
}

export const EMPTY_EXPORT_FORM: ExportFormState = {
  keyword: '',
  categoryId: '',
  active: '',
  priceMin: '',
  priceMax: '',
  customerStatus: '',
  groupId: '',
  customerId: '',
  orderStatus: '',
  paymentStatus: '',
  dateFrom: '',
  dateTo: '',
  groupBy: 'DAY',
  limit: '10',
}
