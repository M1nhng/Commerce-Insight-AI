/**
 * features/export/components/ExportSummary.tsx
 *
 * Compact, informational recap of what will be exported. No row counts, no
 * extra API calls — just the chosen report, format and active filters.
 */
import {
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  type OrderStatus,
  type PaymentStatus,
} from '@/types/order.types'
import type { CustomerStatus } from '@/types/customer.types'
import { EXPORT_REPORT_MAP } from '../constants'
import type { ExportFormState, ExportFormat, ExportReportType } from '../types/export.types'
import { displayDate } from '../utils/dateRange'

interface Props {
  reportType: ExportReportType
  format: ExportFormat
  form: ExportFormState
  categoryLabel?: string
  groupLabel?: string
  customerLabel?: string
}

const CUSTOMER_STATUS_LABELS: Record<CustomerStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  BLOCKED: 'Blocked',
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-caption" style={{ color: 'var(--text-muted)' }}>
        {label}
      </span>
      <span
        className="text-body-sm font-medium text-right"
        style={{ color: 'var(--text-primary)' }}
      >
        {value}
      </span>
    </div>
  )
}

export function ExportSummary({
  reportType,
  format,
  form,
  categoryLabel,
  groupLabel,
  customerLabel,
}: Props) {
  const meta = EXPORT_REPORT_MAP[reportType]
  const filters: { label: string; value: string }[] = []

  const add = (label: string, value: string | undefined | null) => {
    if (value && value.trim() !== '') filters.push({ label, value })
  }

  switch (reportType) {
    case 'PRODUCTS':
      add('Search', form.keyword)
      add('Category', categoryLabel)
      add(
        'Status',
        form.active === 'true' ? 'Active only' : form.active === 'false' ? 'Inactive only' : undefined,
      )
      add('Min price', form.priceMin)
      add('Max price', form.priceMax)
      break
    case 'CUSTOMERS':
      add('Keyword', form.keyword)
      add(
        'Status',
        form.customerStatus ? CUSTOMER_STATUS_LABELS[form.customerStatus as CustomerStatus] : undefined,
      )
      add('Group', groupLabel)
      add('Created from', displayDate(form.dateFrom))
      add('Created to', displayDate(form.dateTo))
      break
    case 'ORDERS':
      add('Keyword', form.keyword)
      add('Customer', customerLabel)
      add(
        'Status',
        form.orderStatus ? ORDER_STATUS_LABELS[form.orderStatus as OrderStatus] : undefined,
      )
      add(
        'Payment',
        form.paymentStatus
          ? PAYMENT_STATUS_LABELS[form.paymentStatus as PaymentStatus]
          : undefined,
      )
      add('Created from', displayDate(form.dateFrom))
      add('Created to', displayDate(form.dateTo))
      break
    case 'REVENUE':
      add('From', displayDate(form.dateFrom))
      add('To', displayDate(form.dateTo))
      add('Group by', form.groupBy.charAt(0) + form.groupBy.slice(1).toLowerCase())
      break
    case 'TOP_PRODUCTS':
      add('From', displayDate(form.dateFrom))
      add('To', displayDate(form.dateTo))
      add('Products', form.limit)
      break
    case 'ORDER_ANALYTICS':
    case 'CUSTOMER_ANALYTICS':
    case 'PAYMENT_ANALYTICS':
      add('From', displayDate(form.dateFrom))
      add('To', displayDate(form.dateTo))
      break
  }

  return (
    <div className="space-y-2">
      <Row label="Report" value={meta.label} />
      <Row label="Format" value={format} />
      <div className="h-px" style={{ background: 'var(--border-subtle)' }} />
      {filters.length === 0 ? (
        <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
          No filters applied — the full data set will be exported (subject to the
          server row limit).
        </p>
      ) : (
        filters.map((f) => <Row key={f.label} label={f.label} value={f.value} />)
      )}
    </div>
  )
}
