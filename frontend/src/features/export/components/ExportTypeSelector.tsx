/**
 * features/export/components/ExportTypeSelector.tsx
 *
 * Card grid of exportable reports, grouped into Catalog / Analytics.
 * Follows the same visual pattern as ImportTypeSelector.
 */
import {
  Package,
  Users,
  ShoppingCart,
  TrendingUp,
  ClipboardList,
  Trophy,
  UserCheck,
  CreditCard,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { EXPORT_REPORTS } from '../constants'
import type { ExportReportCategory, ExportReportType } from '../types/export.types'

const ICONS: Record<ExportReportType, React.ElementType> = {
  PRODUCTS: Package,
  CUSTOMERS: Users,
  ORDERS: ShoppingCart,
  REVENUE: TrendingUp,
  ORDER_ANALYTICS: ClipboardList,
  TOP_PRODUCTS: Trophy,
  CUSTOMER_ANALYTICS: UserCheck,
  PAYMENT_ANALYTICS: CreditCard,
}

const GROUPS: { category: ExportReportCategory; label: string }[] = [
  { category: 'CATALOG', label: 'Catalog' },
  { category: 'ANALYTICS', label: 'Analytics' },
]

interface Props {
  value: ExportReportType
  onChange: (type: ExportReportType) => void
  disabled?: boolean
}

export function ExportTypeSelector({ value, onChange, disabled }: Props) {
  return (
    <div className="space-y-4">
      {GROUPS.map((group) => {
        const reports = EXPORT_REPORTS.filter((r) => r.category === group.category)
        return (
          <div key={group.category} className="space-y-2">
            <p
              className="text-caption font-semibold tracking-widest"
              style={{ color: 'var(--text-muted)' }}
            >
              {group.label.toUpperCase()}
            </p>
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-3">
              {reports.map((report) => {
                const Icon = ICONS[report.type]
                const isSelected = value === report.type
                return (
                  <button
                    key={report.type}
                    type="button"
                    id={`export-type-${report.type.toLowerCase()}`}
                    disabled={disabled}
                    aria-pressed={isSelected}
                    onClick={() => onChange(report.type)}
                    className={cn(
                      'flex items-start gap-3 rounded-xl border px-3.5 py-3 text-left transition-all duration-150',
                      'focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent-500)]',
                      disabled
                        ? 'cursor-not-allowed opacity-50'
                        : 'cursor-pointer hover:opacity-95',
                    )}
                    style={{
                      background: isSelected
                        ? 'rgba(99,102,241,0.08)'
                        : 'var(--bg-elevated)',
                      borderColor: isSelected
                        ? 'var(--accent-500)'
                        : 'var(--border-default)',
                      boxShadow: isSelected ? '0 0 0 1px var(--accent-500)' : 'none',
                    }}
                  >
                    <div
                      className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg"
                      style={{
                        background: isSelected
                          ? 'rgba(99,102,241,0.16)'
                          : 'var(--bg-overlay)',
                      }}
                    >
                      <Icon
                        className="h-4 w-4"
                        style={{
                          color: isSelected ? 'var(--accent-400)' : 'var(--text-muted)',
                        }}
                      />
                    </div>
                    <div className="min-w-0">
                      <p
                        className="text-body-sm font-semibold"
                        style={{
                          color: isSelected
                            ? 'var(--accent-400)'
                            : 'var(--text-primary)',
                        }}
                      >
                        {report.label}
                      </p>
                      <p
                        className="text-caption mt-0.5 leading-snug"
                        style={{ color: 'var(--text-muted)' }}
                      >
                        {report.description}
                      </p>
                    </div>
                  </button>
                )
              })}
            </div>
          </div>
        )
      })}
    </div>
  )
}