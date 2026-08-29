/**
 * features/export/services/exportService.ts
 *
 * Calls the Sprint 11A binary export endpoints (GET /api/v1/export/**) using the
 * shared apiClient. Every request uses `responseType: 'blob'` because the backend
 * returns a file, not JSON.
 *
 * Rules:
 * - No second Axios instance, no manual JWT — the shared apiClient handles auth.
 * - Undefined / null / empty optional params are never sent (cleanParams).
 * - On failure a safe Error(message) is thrown; blob error bodies are decoded.
 */
import type { AxiosResponse } from 'axios'
import { apiClient } from '@/services/axios'
import {
  EXPORT_FORMAT_PARAM,
  type AnalyticsExportParams,
  type CustomerExportParams,
  type ExportDownloadResult,
  type ExportFormat,
  type ExportRequest,
  type OrderExportParams,
  type ProductExportParams,
  type RevenueExportParams,
  type TopProductsExportParams,
} from '../types/export.types'
import { EXPORT_REPORT_MAP } from '../constants'
import {
  extractFilename,
  fallbackFilename,
  parseBlobError,
  triggerBrowserDownload,
} from '../utils/download'

const BASE = '/export'

/** Drop null / undefined / '' so Axios never serialises them into the query. */
function cleanParams(params: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, v]) => v !== null && v !== undefined && v !== '',
    ),
  )
}

/** Map an HTTP status to a safe, user-readable fallback message. */
function messageForStatus(status: number | undefined): string {
  switch (status) {
    case 400:
      return 'Invalid export request. Please check your filters.'
    case 401:
      return 'Your session has expired. Please sign in again.'
    case 403:
      return 'You do not have permission to export reports.'
    case 404:
      return 'Export endpoint not found.'
    case 422:
      return 'This export could not be completed. Please adjust your filters.'
    case 429:
      return 'Too many export requests. Please wait a moment and try again.'
    default:
      if (status && status >= 500) {
        return 'Unable to generate the report right now. Please try again.'
      }
      return 'Unable to reach the backend. Please check your connection.'
  }
}

/**
 * Runs one export request end-to-end: GET → blob → filename → browser download.
 * Throws Error(safeMessage) on any failure.
 */
async function runExport(
  path: string,
  params: Record<string, unknown>,
  fallbackBase: string,
  format: ExportFormat,
): Promise<ExportDownloadResult> {
  let response: AxiosResponse<Blob>
  try {
    response = await apiClient.get<Blob>(path, {
      params: cleanParams(params),
      responseType: 'blob',
    })
  } catch (err) {
    const axiosErr = err as {
      response?: { status?: number; data?: unknown }
      code?: string
    }
    const backendMessage = await parseBlobError(axiosErr.response?.data)
    throw new Error(backendMessage ?? messageForStatus(axiosErr.response?.status))
  }

  const blob = response.data
  // A 2xx with a JSON body means the server reported a soft failure as a blob.
  if (blob.type.includes('application/json')) {
    const backendMessage = await parseBlobError(blob)
    throw new Error(backendMessage ?? messageForStatus(response.status))
  }

  const filename = extractFilename(
    (response.headers?.['content-disposition'] as string | undefined) ?? undefined,
    fallbackFilename(fallbackBase, format),
  )

  triggerBrowserDownload(blob, filename)
  return { filename, format, sizeBytes: blob.size }
}

function fmt(format: ExportFormat): string {
  return EXPORT_FORMAT_PARAM[format]
}

export const exportService = {
  exportProducts(format: ExportFormat, params: ProductExportParams) {
    return runExport(
      `${BASE}/products`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.PRODUCTS.fallbackBase,
      format,
    )
  },

  exportCustomers(format: ExportFormat, params: CustomerExportParams) {
    return runExport(
      `${BASE}/customers`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.CUSTOMERS.fallbackBase,
      format,
    )
  },

  exportOrders(format: ExportFormat, params: OrderExportParams) {
    return runExport(
      `${BASE}/orders`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.ORDERS.fallbackBase,
      format,
    )
  },

  exportRevenueAnalytics(format: ExportFormat, params: RevenueExportParams) {
    return runExport(
      `${BASE}/analytics/revenue`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.REVENUE.fallbackBase,
      format,
    )
  },

  exportOrderAnalytics(format: ExportFormat, params: AnalyticsExportParams) {
    return runExport(
      `${BASE}/analytics/orders`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.ORDER_ANALYTICS.fallbackBase,
      format,
    )
  },

  exportTopProducts(format: ExportFormat, params: TopProductsExportParams) {
    return runExport(
      `${BASE}/analytics/products`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.TOP_PRODUCTS.fallbackBase,
      format,
    )
  },

  exportCustomerAnalytics(format: ExportFormat, params: AnalyticsExportParams) {
    return runExport(
      `${BASE}/analytics/customers`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.CUSTOMER_ANALYTICS.fallbackBase,
      format,
    )
  },

  exportPaymentAnalytics(format: ExportFormat, params: AnalyticsExportParams) {
    return runExport(
      `${BASE}/analytics/payments`,
      { format: fmt(format), ...params },
      EXPORT_REPORT_MAP.PAYMENT_ANALYTICS.fallbackBase,
      format,
    )
  },

  /** Dispatch a discriminated ExportRequest to the matching method. */
  run(request: ExportRequest): Promise<ExportDownloadResult> {
    switch (request.type) {
      case 'PRODUCTS':
        return this.exportProducts(request.format, request.params)
      case 'CUSTOMERS':
        return this.exportCustomers(request.format, request.params)
      case 'ORDERS':
        return this.exportOrders(request.format, request.params)
      case 'REVENUE':
        return this.exportRevenueAnalytics(request.format, request.params)
      case 'ORDER_ANALYTICS':
        return this.exportOrderAnalytics(request.format, request.params)
      case 'TOP_PRODUCTS':
        return this.exportTopProducts(request.format, request.params)
      case 'CUSTOMER_ANALYTICS':
        return this.exportCustomerAnalytics(request.format, request.params)
      case 'PAYMENT_ANALYTICS':
        return this.exportPaymentAnalytics(request.format, request.params)
    }
  },
}
