/**
 * features/export/components/OrderExportFilters.tsx
 *
 * Filters for GET /api/v1/export/orders — mirrors the Order list filters.
 * Customer selection reuses the customers list query (keyword search → pick).
 */
import { useEffect, useMemo, useState } from 'react'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useCustomers } from '@/features/customers/hooks/useCustomers'
import {
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  type OrderStatus,
  type PaymentStatus,
} from '@/types/order.types'
import type { ExportFormState } from '../types/export.types'
import { ExportDateRange } from './ExportDateRange'

interface Props {
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
  /** Reports the human label of the picked customer (for the summary panel). */
  onCustomerLabelChange?: (label: string | null) => void
}

const NONE = '__none__'

// Order export status filter — backend OrderStatus minus REFUNDED per Sprint 11B spec.
const ORDER_STATUS_OPTIONS: OrderStatus[] = [
  'PENDING',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
]

const PAYMENT_STATUS_OPTIONS: PaymentStatus[] = ['PENDING', 'PAID', 'FAILED', 'REFUNDED']

export function OrderExportFilters({
  value,
  onChange,
  disabled,
  onCustomerLabelChange,
}: Props) {
  const [customerSearch, setCustomerSearch] = useState('')
  const [debounced, setDebounced] = useState('')

  useEffect(() => {
    const t = setTimeout(() => setDebounced(customerSearch.trim()), 300)
    return () => clearTimeout(t)
  }, [customerSearch])

  const { data: customerPage, isFetching: customersLoading } = useCustomers({
    keyword: debounced || undefined,
    size: 20,
    sortBy: 'createdAt',
    sortDir: 'desc',
  })

  const customerOptions = useMemo(
    () =>
      (customerPage?.content ?? []).map((c) => ({
        value: c.id,
        label: `${c.fullName}${c.customerCode ? ` · ${c.customerCode}` : ''}`,
      })),
    [customerPage],
  )

  const inputStyle = {
    background: 'var(--bg-overlay)',
    borderColor: 'var(--border-default)',
    color: 'var(--text-primary)',
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {/* Keyword */}
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="export-order-keyword" style={{ color: 'var(--text-secondary)' }}>
            Keyword
          </Label>
          <Input
            id="export-order-keyword"
            placeholder="Order number"
            value={value.keyword}
            disabled={disabled}
            onChange={(e) => onChange({ keyword: e.target.value })}
            style={inputStyle}
          />
        </div>

        {/* Status */}
        <div className="space-y-1.5">
          <Label htmlFor="export-order-status" style={{ color: 'var(--text-secondary)' }}>
            Order status
          </Label>
          <Select
            value={value.orderStatus === '' ? NONE : value.orderStatus}
            disabled={disabled}
            onValueChange={(v) => onChange({ orderStatus: v === NONE ? '' : v })}
          >
            <SelectTrigger id="export-order-status" style={inputStyle}>
              <SelectValue placeholder="Any status" />
            </SelectTrigger>
            <SelectContent
              style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
            >
              <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
                Any status
              </SelectItem>
              {ORDER_STATUS_OPTIONS.map((s) => (
                <SelectItem key={s} value={s} style={{ color: 'var(--text-primary)' }}>
                  {ORDER_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Payment status */}
        <div className="space-y-1.5">
          <Label htmlFor="export-order-payment" style={{ color: 'var(--text-secondary)' }}>
            Payment status
          </Label>
          <Select
            value={value.paymentStatus === '' ? NONE : value.paymentStatus}
            disabled={disabled}
            onValueChange={(v) => onChange({ paymentStatus: v === NONE ? '' : v })}
          >
            <SelectTrigger id="export-order-payment" style={inputStyle}>
              <SelectValue placeholder="Any payment status" />
            </SelectTrigger>
            <SelectContent
              style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
            >
              <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
                Any payment status
              </SelectItem>
              {PAYMENT_STATUS_OPTIONS.map((s) => (
                <SelectItem key={s} value={s} style={{ color: 'var(--text-primary)' }}>
                  {PAYMENT_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Customer picker */}
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="export-order-customer-search" style={{ color: 'var(--text-secondary)' }}>
            Customer
          </Label>
          <Input
            id="export-order-customer-search"
            placeholder="Search customers to filter by one…"
            value={customerSearch}
            disabled={disabled}
            onChange={(e) => setCustomerSearch(e.target.value)}
            style={inputStyle}
          />
          <Select
            value={value.customerId === '' ? NONE : value.customerId}
            disabled={disabled || customersLoading}
            onValueChange={(v) => {
              const id = v === NONE ? '' : v
              onChange({ customerId: id })
              onCustomerLabelChange?.(
                id === '' ? null : (customerOptions.find((o) => o.value === id)?.label ?? null),
              )
            }}
          >
            <SelectTrigger id="export-order-customer" style={inputStyle}>
              <SelectValue placeholder="All customers" />
            </SelectTrigger>
            <SelectContent
              style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
            >
              <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
                All customers
              </SelectItem>
              {customerOptions.map((opt) => (
                <SelectItem key={opt.value} value={opt.value} style={{ color: 'var(--text-primary)' }}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <ExportDateRange
        from={value.dateFrom}
        to={value.dateTo}
        onChange={onChange}
        disabled={disabled}
        fromLabel="Created from"
        toLabel="Created to"
        idPrefix="export-order"
      />
    </div>
  )
}
