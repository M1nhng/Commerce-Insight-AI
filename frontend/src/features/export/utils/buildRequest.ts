/**
 * features/export/utils/buildRequest.ts
 *
 * Converts the flat UI form state into a typed, cleaned ExportRequest, and
 * validates a form for a given report before the request is allowed.
 *
 * Only the selected report's supported fields are ever read here, so a value
 * from one report can never be sent to another endpoint.
 */
import {
  TOP_PRODUCTS_LIMIT_DEFAULT,
  TOP_PRODUCTS_LIMIT_MAX,
  TOP_PRODUCTS_LIMIT_MIN,
} from '../constants'
import type { CustomerStatus } from '@/types/customer.types'
import type { OrderStatus, PaymentStatus } from '@/types/order.types'
import type {
  ExportFormState,
  ExportFormat,
  ExportReportType,
  ExportRequest,
} from '../types/export.types'
import { isValidRange, toEndInstant, toStartInstant } from './dateRange'

/** '' → undefined, otherwise the trimmed string. */
function str(v: string): string | undefined {
  const t = v.trim()
  return t === '' ? undefined : t
}

/** '' or non-numeric → undefined, otherwise the number. */
function num(v: string): number | undefined {
  const t = v.trim()
  if (t === '') return undefined
  const n = Number(t)
  return Number.isFinite(n) ? n : undefined
}

function boolOrUndef(v: string): boolean | undefined {
  if (v === 'true') return true
  if (v === 'false') return false
  return undefined
}

export function buildExportRequest(
  reportType: ExportReportType,
  format: ExportFormat,
  form: ExportFormState,
): ExportRequest {
  const dateFrom = toStartInstant(form.dateFrom)
  const dateTo = toEndInstant(form.dateTo)

  switch (reportType) {
    case 'PRODUCTS':
      return {
        type: 'PRODUCTS',
        format,
        params: {
          search: str(form.keyword),
          categoryId: str(form.categoryId),
          active: boolOrUndef(form.active),
          priceMin: num(form.priceMin),
          priceMax: num(form.priceMax),
        },
      }
    case 'CUSTOMERS':
      return {
        type: 'CUSTOMERS',
        format,
        params: {
          keyword: str(form.keyword),
          status: str(form.customerStatus) as CustomerStatus | undefined,
          groupId: str(form.groupId),
          startDate: dateFrom,
          endDate: dateTo,
        },
      }
    case 'ORDERS':
      return {
        type: 'ORDERS',
        format,
        params: {
          keyword: str(form.keyword),
          customerId: str(form.customerId),
          status: str(form.orderStatus) as OrderStatus | undefined,
          paymentStatus: str(form.paymentStatus) as PaymentStatus | undefined,
          dateFrom,
          dateTo,
        },
      }
    case 'REVENUE':
      return {
        type: 'REVENUE',
        format,
        params: { dateFrom, dateTo, groupBy: form.groupBy },
      }
    case 'TOP_PRODUCTS': {
      const parsed = num(form.limit)
      return {
        type: 'TOP_PRODUCTS',
        format,
        params: { dateFrom, dateTo, limit: parsed ?? TOP_PRODUCTS_LIMIT_DEFAULT },
      }
    }
    case 'ORDER_ANALYTICS':
      return { type: 'ORDER_ANALYTICS', format, params: { dateFrom, dateTo } }
    case 'CUSTOMER_ANALYTICS':
      return { type: 'CUSTOMER_ANALYTICS', format, params: { dateFrom, dateTo } }
    case 'PAYMENT_ANALYTICS':
      return { type: 'PAYMENT_ANALYTICS', format, params: { dateFrom, dateTo } }
  }
}

/** Returns a user-readable message when the form is not exportable, else null. */
export function validateExportForm(
  reportType: ExportReportType,
  form: ExportFormState,
): string | null {
  if (reportType === 'PRODUCTS') {
    const min = num(form.priceMin)
    const max = num(form.priceMax)
    if ((form.priceMin.trim() !== '' && min === undefined) || (min !== undefined && min < 0)) {
      return 'Minimum price must be a number of 0 or more.'
    }
    if ((form.priceMax.trim() !== '' && max === undefined) || (max !== undefined && max < 0)) {
      return 'Maximum price must be a number of 0 or more.'
    }
    if (min !== undefined && max !== undefined && min > max) {
      return 'Minimum price must be less than or equal to the maximum price.'
    }
    return null
  }

  if (reportType === 'TOP_PRODUCTS') {
    const n = num(form.limit)
    if (
      form.limit.trim() === '' ||
      n === undefined ||
      !Number.isInteger(n) ||
      n < TOP_PRODUCTS_LIMIT_MIN ||
      n > TOP_PRODUCTS_LIMIT_MAX
    ) {
      return `Number of products must be a whole number between ${TOP_PRODUCTS_LIMIT_MIN} and ${TOP_PRODUCTS_LIMIT_MAX}.`
    }
  }

  // Every remaining report supports a date range.
  if (!isValidRange(form.dateFrom, form.dateTo)) {
    return 'The start date must be on or before the end date.'
  }

  return null
}
