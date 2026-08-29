/**
 * features/export/components/AnalyticsExportFilters.tsx
 *
 * Date-range-only filters, shared by:
 *   GET /api/v1/export/analytics/orders
 *   GET /api/v1/export/analytics/customers
 *   GET /api/v1/export/analytics/payments
 */
import type { ExportFormState } from '../types/export.types'
import { ExportDateRange } from './ExportDateRange'

interface Props {
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
}

export function AnalyticsExportFilters({ value, onChange, disabled }: Props) {
  return (
    <ExportDateRange
      from={value.dateFrom}
      to={value.dateTo}
      onChange={onChange}
      disabled={disabled}
      idPrefix="export-analytics"
    />
  )
}
