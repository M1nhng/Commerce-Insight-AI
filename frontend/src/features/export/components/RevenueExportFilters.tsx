/**
 * features/export/components/RevenueExportFilters.tsx
 *
 * Filters for GET /api/v1/export/analytics/revenue — date range + groupBy.
 */
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { ExportFormState, RevenueGroupBy } from '../types/export.types'
import { ExportDateRange } from './ExportDateRange'

interface Props {
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
}

const GROUP_BY_OPTIONS: { value: RevenueGroupBy; label: string }[] = [
  { value: 'DAY', label: 'Day' },
  { value: 'WEEK', label: 'Week' },
  { value: 'MONTH', label: 'Month' },
]

export function RevenueExportFilters({ value, onChange, disabled }: Props) {
  const inputStyle = {
    background: 'var(--bg-overlay)',
    borderColor: 'var(--border-default)',
    color: 'var(--text-primary)',
  }

  return (
    <div className="space-y-4">
      <ExportDateRange
        from={value.dateFrom}
        to={value.dateTo}
        onChange={onChange}
        disabled={disabled}
        idPrefix="export-revenue"
      />

      <div className="space-y-1.5 sm:max-w-[12rem]">
        <Label htmlFor="export-revenue-groupby" style={{ color: 'var(--text-secondary)' }}>
          Group by
        </Label>
        <Select
          value={value.groupBy}
          disabled={disabled}
          onValueChange={(v) => onChange({ groupBy: v as RevenueGroupBy })}
        >
          <SelectTrigger id="export-revenue-groupby" style={inputStyle}>
            <SelectValue />
          </SelectTrigger>
          <SelectContent
            style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
          >
            {GROUP_BY_OPTIONS.map((opt) => (
              <SelectItem key={opt.value} value={opt.value} style={{ color: 'var(--text-primary)' }}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  )
}
