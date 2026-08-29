/**
 * features/export/components/ExportFormatSelector.tsx
 *
 * Two-option format picker (XLSX / PDF). Implemented as an accessible radio
 * group. Changing the format never touches the filters.
 */
import { FileSpreadsheet, FileText } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ExportFormat } from '../types/export.types'

const OPTIONS: {
  value: ExportFormat
  label: string
  hint: string
  icon: React.ElementType
}[] = [
  {
    value: 'XLSX',
    label: 'XLSX',
    hint: 'Best for analysis and editing',
    icon: FileSpreadsheet,
  },
  {
    value: 'PDF',
    label: 'PDF',
    hint: 'Best for sharing and printing',
    icon: FileText,
  },
]

interface Props {
  value: ExportFormat
  onChange: (format: ExportFormat) => void
  disabled?: boolean
}

export function ExportFormatSelector({ value, onChange, disabled }: Props) {
  return (
    <div
      role="radiogroup"
      aria-label="Export format"
      className="grid grid-cols-1 gap-2.5 sm:grid-cols-2"
    >
      {OPTIONS.map((opt) => {
        const isSelected = value === opt.value
        const Icon = opt.icon
        return (
          <button
            key={opt.value}
            type="button"
            role="radio"
            aria-checked={isSelected}
            id={`export-format-${opt.value.toLowerCase()}`}
            disabled={disabled}
            onClick={() => onChange(opt.value)}
            className={cn(
              'flex items-center gap-3 rounded-xl border px-4 py-3 text-left transition-all duration-150',
              'focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--accent-500)]',
              disabled
                ? 'cursor-not-allowed opacity-50'
                : 'cursor-pointer hover:opacity-95',
            )}
            style={{
              background: isSelected ? 'rgba(99,102,241,0.08)' : 'var(--bg-elevated)',
              borderColor: isSelected ? 'var(--accent-500)' : 'var(--border-default)',
              boxShadow: isSelected ? '0 0 0 1px var(--accent-500)' : 'none',
            }}
          >
            <Icon
              className="h-5 w-5 shrink-0"
              style={{
                color: isSelected ? 'var(--accent-400)' : 'var(--text-muted)',
              }}
            />
            <div>
              <p
                className="text-body-sm font-semibold"
                style={{
                  color: isSelected ? 'var(--accent-400)' : 'var(--text-primary)',
                }}
              >
                {opt.label}
              </p>
              <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
                {opt.hint}
              </p>
            </div>
          </button>
        )
      })}
    </div>
  )
}