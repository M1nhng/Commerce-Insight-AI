/**
 * features/export/components/ProductExportFilters.tsx
 *
 * Filters for GET /api/v1/export/products — mirrors the Product list filters.
 * Category options are reused from the products feature (no duplicate API call
 * pattern).
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
import { useCategoryOptions } from '@/features/products/hooks/useCategories'
import type { ExportFormState } from '../types/export.types'

interface Props {
  value: ExportFormState
  onChange: (patch: Partial<ExportFormState>) => void
  disabled?: boolean
}

const NONE = '__none__'

export function ProductExportFilters({ value, onChange, disabled }: Props) {
  const { data: categoryOptions = [], isLoading: categoriesLoading } = useCategoryOptions()

  const priceInvalid =
    value.priceMin !== '' &&
    value.priceMax !== '' &&
    Number(value.priceMin) > Number(value.priceMax)

  const inputStyle = {
    background: 'var(--bg-overlay)',
    borderColor: 'var(--border-default)',
    color: 'var(--text-primary)',
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      {/* Search */}
      <div className="space-y-1.5 sm:col-span-2">
        <Label htmlFor="export-product-search" style={{ color: 'var(--text-secondary)' }}>
          Search
        </Label>
        <Input
          id="export-product-search"
          placeholder="Name or SKU"
          value={value.keyword}
          disabled={disabled}
          onChange={(e) => onChange({ keyword: e.target.value })}
          style={inputStyle}
        />
      </div>

      {/* Category */}
      <div className="space-y-1.5">
        <Label htmlFor="export-product-category" style={{ color: 'var(--text-secondary)' }}>
          Category
        </Label>
        <Select
          value={value.categoryId === '' ? NONE : value.categoryId}
          disabled={disabled || categoriesLoading}
          onValueChange={(v) => onChange({ categoryId: v === NONE ? '' : v })}
        >
          <SelectTrigger id="export-product-category" style={inputStyle}>
            <SelectValue placeholder="All categories" />
          </SelectTrigger>
          <SelectContent
            style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
          >
            <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
              All categories
            </SelectItem>
            {categoryOptions.map((opt) => (
              <SelectItem key={opt.value} value={opt.value} style={{ color: 'var(--text-primary)' }}>
                {opt.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Active status */}
      <div className="space-y-1.5">
        <Label htmlFor="export-product-active" style={{ color: 'var(--text-secondary)' }}>
          Status
        </Label>
        <Select
          value={value.active === '' ? NONE : value.active}
          disabled={disabled}
          onValueChange={(v) => onChange({ active: v === NONE ? '' : v })}
        >
          <SelectTrigger id="export-product-active" style={inputStyle}>
            <SelectValue placeholder="Any status" />
          </SelectTrigger>
          <SelectContent
            style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
          >
            <SelectItem value={NONE} style={{ color: 'var(--text-primary)' }}>
              Any status
            </SelectItem>
            <SelectItem value="true" style={{ color: 'var(--text-primary)' }}>
              Active only
            </SelectItem>
            <SelectItem value="false" style={{ color: 'var(--text-primary)' }}>
              Inactive only
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Price min */}
      <div className="space-y-1.5">
        <Label htmlFor="export-product-price-min" style={{ color: 'var(--text-secondary)' }}>
          Minimum price
        </Label>
        <Input
          id="export-product-price-min"
          type="number"
          min={0}
          step="0.01"
          placeholder="0.00"
          value={value.priceMin}
          disabled={disabled}
          onChange={(e) => onChange({ priceMin: e.target.value })}
          style={{
            ...inputStyle,
            borderColor: priceInvalid ? 'var(--error)' : 'var(--border-default)',
          }}
        />
      </div>

      {/* Price max */}
      <div className="space-y-1.5">
        <Label htmlFor="export-product-price-max" style={{ color: 'var(--text-secondary)' }}>
          Maximum price
        </Label>
        <Input
          id="export-product-price-max"
          type="number"
          min={0}
          step="0.01"
          placeholder="0.00"
          value={value.priceMax}
          disabled={disabled}
          onChange={(e) => onChange({ priceMax: e.target.value })}
          style={{
            ...inputStyle,
            borderColor: priceInvalid ? 'var(--error)' : 'var(--border-default)',
          }}
        />
      </div>

      {priceInvalid && (
        <p
          className="text-caption sm:col-span-2"
          style={{ color: 'var(--error)' }}
          role="alert"
        >
          Minimum price must be less than or equal to the maximum price.
        </p>
      )}
    </div>
  )
}