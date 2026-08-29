/**
 * features/export/components/TopProductsExportFilters.tsx
 *
 * Filters for GET /api/v1/export/analytics/products — date range + limit (1–100).
 */
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  TOP_PRODUCTS_LIMIT_MAX,
  TOP_PRODUCTS_LIMIT_MIN,
} from '../constants'
import type { ExportFormState } from '../types/export.types'
import { ExportDateRange } from './ExportDateRange'

interface Props {
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
}

export function TopProductsExportFilters({ value, onChange, disabled }: Props) {
  const n = Number(value.limit)
  const limitInvalid =
    value.limit.trim() === '' ||
    !Number.isInteger(n) ||
    n < TOP_PRODUCTS_LIMIT_MIN ||
    n > TOP_PRODUCTS_LIMIT_MAX

  return (
    <div className="space-y-4">
      <ExportDateRange
        from={value.dateFrom}
        to={value.dateTo}
        onChange={onChange}
        disabled={disabled}
        idPrefix="export-topproducts"
      />

      <div className="space-y-1.5 sm:max-w-[12rem]">
        <Label htmlFor="export-topproducts-limit" style={{ color: 'var(--text-secondary)' }}>
          Number of products
        </Label>
        <Input
          id="export-topproducts-limit"
          type="number"
          min={TOP_PRODUCTS_LIMIT_MIN}
          max={TOP_PRODUCTS_LIMIT_MAX}
          step={1}
          value={value.limit}
          disabled={disabled}
          onChange={(e) => onChange({ limit: e.target.value })}
          style={{
            background: 'var(--bg-overlay)',
            borderColor: limitInvalid ? 'var(--error)' : 'var(--border-default)',
            color: 'var(--text-primary)',
          }}
        />
        <p
          className="text-caption"
          style={{ color: limitInvalid ? 'var(--error)' : 'var(--text-muted)' }}
          role={limitInvalid ? 'alert' : undefined}
        >
          Enter a whole number between {TOP_PRODUCTS_LIMIT_MIN} and {TOP_PRODUCTS_LIMIT_MAX}.
        </p>
      </div>
    </div>
  )
}
