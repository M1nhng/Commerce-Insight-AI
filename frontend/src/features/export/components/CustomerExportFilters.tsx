/**
 * features/export/components/CustomerExportFilters.tsx
 *
 * Filters for GET /api/v1/export/customers — mirrors the Customer list filters.
 * Group options reused from the customers feature.
 */
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useCustomerGroupOptions } from '@/features/customers/hooks/useCustomerGroups'
import type { CustomerStatus } from '@/types/customer.types'
import type { ExportFormState } from '../types/export.types'
import { ExportDateRange } from './ExportDateRange'

interface Props {
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
}

const NONE = '__none__'

const STATUS_OPTIONS: { value: CustomerStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'BLOCKED', label: 'Blocked' },
]

export function CustomerExportFilters({ value, onChange, disabled }: Props) {
  const { data: groupOptions = [], isLoading: groupsLoading } = useCustomerGroupOptions()

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
          <Label htmlFor="export-customer-keyword" style={{ color: 'var(--text-secondary)' }}>
            Keyword
          </Label>
          <Input
            id="export-customer-keyword"
            placeholder="Name, email, phone or code"
            value={value.keyword}
            disabled={disabled}
            onChange={(e) => onChange({ keyword: e.target.value })}
            style={inputStyle}
          />
        </div>

        {/* Status */}
        <div className="space-y-1.5">
          <Label htmlFor="export-customer-status" style={{ color: 'var(--text-secondary)' }}>
            Status
          </Label>
          <Select
            value={value.customerStatus === '' ? NONE : value.customerStatus}
            disabled={disabled}
            onValueChange={(v) => onChange({ customerStatus: v === NONE ? '' : v })}
          >
            <SelectTrigger id="export-customer-status" style={inputStyle}>
              <SelectValue placeholder="Any status" />
            </SelectTrigger>
            <SelectContent
              style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
            >
              <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
                Any status
              </SelectItem>
              {STATUS_OPTIONS.map((opt) => (
                <SelectItem key={opt.value} value={opt.value} style={{ color: 'var(--text-primary)' }}>
                  {opt.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Group */}
        <div className="space-y-1.5">
          <Label htmlFor="export-customer-group" style={{ color: 'var(--text-secondary)' }}>
            Customer group
          </Label>
          <Select
            value={value.groupId === '' ? NONE : value.groupId}
            disabled={disabled || groupsLoading}
            onValueChange={(v) => onChange({ groupId: v === NONE ? '' : v })}
          >
            <SelectTrigger id="export-customer-group" style={inputStyle}>
              <SelectValue placeholder="All groups" />
            </SelectTrigger>
            <SelectContent
              style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
            >
              <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
                All groups
              </SelectItem>
              {groupOptions.map((opt) => (
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
        idPrefix="export-customer"
      />
    </div>
  )
}