/**
 * features/export/components/ExportDateRange.tsx
 *
 * Reusable "from / to" date range with inline validation. Emits raw yyyy-mm-dd
 * strings; conversion to ISO-8601 instants happens at submit time.
 */
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { isValidRange } from '../utils/dateRange'

interface Props {
  from: string
  to: string
  onChange: (patch: { dateFrom?: string; dateTo?: string }) => void
  disabled?: boolean
  fromLabel?: string
  toLabel?: string
  idPrefix?: string
}

export function ExportDateRange({
  from,
  to,
  onChange,
  disabled,
  fromLabel = 'From',
  toLabel = 'To',
  idPrefix = 'export',
}: Props) {
  const invalid = !isValidRange(from, to)

  const fieldStyle = {
    background: 'var(--bg-overlay)',
    borderColor: invalid ? 'var(--error)' : 'var(--border-default)',
    color: 'var(--text-primary)',
  }

  return (
    <div className="space-y-1.5">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor={`${idPrefix}-date-from`} style={{ color: 'var(--text-secondary)' }}>
            {fromLabel}
          </Label>
          <Input
            id={`${idPrefix}-date-from`}
            type="date"
            value={from}
            max={to || undefined}
            disabled={disabled}
            onChange={(e) => onChange({ dateFrom: e.target.value })}
            style={fieldStyle}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor={`${idPrefix}-date-to`} style={{ color: 'var(--text-secondary)' }}>
            {toLabel}
          </Label>
          <Input
            id={`${idPrefix}-date-to`}
            type="date"
            value={to}
            min={from || undefined}
            disabled={disabled}
            onChange={(e) => onChange({ dateTo: e.target.value })}
            style={fieldStyle}
          />
        </div>
      </div>
      {invalid && (
        <p className="text-caption" style={{ color: 'var(--error)' }} role="alert">
          The start date must be on or before the end date.
        </p>
      )}
    </div>
  )
}