/**
 * features/import/components/ImportTypeSelector.tsx
 * Card-based import type selector — step 1 of the import flow.
 */
import { Package, Users, ShoppingCart } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ImportType } from '../types/import.types'

interface TypeOption {
  type: ImportType
  label: string
  description: string
  icon: React.ElementType
  color: string
}

const TYPE_OPTIONS: TypeOption[] = [
  {
    type: 'PRODUCT',
    label: 'Products',
    description: 'SKU, name, price, category',
    icon: Package,
    color: 'var(--accent-400)',
  },
  {
    type: 'CUSTOMER',
    label: 'Customers',
    description: 'Name, email, group, gender',
    icon: Users,
    color: 'var(--success)',
  },
  {
    type: 'ORDER',
    label: 'Orders',
    description: 'Order lines grouped by order number',
    icon: ShoppingCart,
    color: 'var(--warning)',
  },
]

interface ImportTypeSelectorProps {
  value: ImportType | null
  onChange: (type: ImportType) => void
  disabled?: boolean
}

export function ImportTypeSelector({ value, onChange, disabled }: ImportTypeSelectorProps) {
  return (
    <div className="space-y-3">
      <p className="text-body-sm font-medium" style={{ color: 'var(--text-secondary)' }}>
        Select data type to import
      </p>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {TYPE_OPTIONS.map((opt) => {
          const isSelected = value === opt.type
          const Icon = opt.icon
          return (
            <button
              key={opt.type}
              id={`import-type-${opt.type.toLowerCase()}`}
              disabled={disabled}
              onClick={() => onChange(opt.type)}
              className={cn(
                'relative flex flex-col items-center gap-3 rounded-xl border px-4 py-5',
                'text-center transition-all duration-150',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
                disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:opacity-90',
              )}
              style={{
                background: isSelected ? 'rgba(99,102,241,0.08)' : 'var(--bg-elevated)',
                borderColor: isSelected ? opt.color : 'var(--border-default)',
                boxShadow: isSelected ? `0 0 0 1px ${opt.color}` : 'none',
              }}
              aria-pressed={isSelected}
            >
              <div
                className="flex h-11 w-11 items-center justify-center rounded-xl"
                style={{
                  background: isSelected ? `${opt.color}20` : 'var(--bg-overlay)',
                }}
              >
                <Icon
                  className="h-5 w-5"
                  style={{ color: isSelected ? opt.color : 'var(--text-muted)' }}
                />
              </div>
              <div>
                <p
                  className="text-body-sm font-semibold"
                  style={{ color: isSelected ? opt.color : 'var(--text-primary)' }}
                >
                  {opt.label}
                </p>
                <p className="text-caption mt-0.5" style={{ color: 'var(--text-muted)' }}>
                  {opt.description}
                </p>
              </div>
              {isSelected && (
                <span
                  className="absolute top-2 right-2 h-2 w-2 rounded-full"
                  style={{ background: opt.color }}
                />
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}
