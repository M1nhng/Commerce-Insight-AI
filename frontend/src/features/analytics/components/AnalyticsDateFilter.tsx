/**
 * features/analytics/components/AnalyticsDateFilter.tsx
 *
 * Date range filter for the analytics dashboard.
 * Supports presets (Today, Last 7 Days, etc.) and custom date input.
 * Converts all dates to UTC ISO 8601 instants before calling the backend.
 */
import { useState } from 'react'
import { CalendarDays, ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import {
  getDateRange,
  toISOInstant,
  formatDisplayDate,
  type DatePreset,
} from '../utils/dateUtils'
import type { AnalyticsDateRange } from '../types/analytics.types'

interface Props {
  value:    AnalyticsDateRange
  onChange: (range: AnalyticsDateRange) => void
}

const PRESETS: { label: string; preset: DatePreset }[] = [
  { label: 'Today',       preset: 'today' },
  { label: 'Yesterday',   preset: 'yesterday' },
  { label: 'Last 7 Days', preset: 'last7days' },
  { label: 'Last 30 Days',preset: 'last30days' },
  { label: 'This Month',  preset: 'thisMonth' },
  { label: 'Last Month',  preset: 'lastMonth' },
  { label: 'All Time',    preset: 'allTime' },
]

export function AnalyticsDateFilter({ value, onChange }: Props) {
  const [activePreset, setActivePreset] = useState<DatePreset | 'custom'>('last30days')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo,   setCustomTo]   = useState('')
  const [showCustom, setShowCustom] = useState(false)

  function handlePreset(preset: DatePreset) {
    const result = getDateRange(preset)
    setActivePreset(preset)
    setShowCustom(false)
    onChange({ dateFrom: result.dateFrom, dateTo: result.dateTo })
  }

  function handleCustomApply() {
    if (!customFrom && !customTo) return
    const from = customFrom
      ? toISOInstant(new Date(customFrom + 'T00:00:00'))
      : null
    const to = customTo
      ? toISOInstant(new Date(customTo + 'T23:59:59'))
      : null
    setActivePreset('custom')
    onChange({ dateFrom: from, dateTo: to })
  }

  const getLabel = () => {
    if (activePreset === 'custom') {
      const from = formatDisplayDate(value.dateFrom)
      const to   = formatDisplayDate(value.dateTo)
      if (from && to) return `${from} – ${to}`
      if (from)       return `From ${from}`
      return 'Custom'
    }
    return PRESETS.find((p) => p.preset === activePreset)?.label ?? 'All Time'
  }

  return (
    <div className="flex items-center gap-2 flex-wrap">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            className="gap-2 h-9 min-w-[160px] justify-between"
            style={{
              background:   'var(--bg-elevated)',
              borderColor:  'var(--border-default)',
              color:        'var(--text-primary)',
            }}
            aria-label="Select date range"
          >
            <div className="flex items-center gap-2">
              <CalendarDays className="h-4 w-4 shrink-0" style={{ color: 'var(--accent-400)' }} />
              <span className="truncate text-sm">{getLabel()}</span>
            </div>
            <ChevronDown className="h-3.5 w-3.5 shrink-0 opacity-60" />
          </Button>
        </DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          className="w-48"
          style={{
            background:  'var(--bg-elevated)',
            border:      '1px solid var(--border-default)',
            color:       'var(--text-primary)',
          }}
        >
          {PRESETS.map(({ label, preset }) => (
            <DropdownMenuItem
              key={preset}
              onSelect={() => handlePreset(preset)}
              className="cursor-pointer text-sm"
              style={
                activePreset === preset
                  ? { color: 'var(--accent-400)', background: 'rgba(99,102,241,0.08)' }
                  : { color: 'var(--text-secondary)' }
              }
            >
              {label}
            </DropdownMenuItem>
          ))}

          <DropdownMenuSeparator style={{ background: 'var(--border-subtle)' }} />

          <DropdownMenuItem
            onSelect={(e) => { e.preventDefault(); setShowCustom((v) => !v) }}
            className="cursor-pointer text-sm"
            style={
              activePreset === 'custom'
                ? { color: 'var(--accent-400)' }
                : { color: 'var(--text-secondary)' }
            }
          >
            Custom Range
          </DropdownMenuItem>

          {showCustom && (
            <div className="px-2 py-2 space-y-2">
              <Input
                type="date"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
                placeholder="From"
                className="h-8 text-xs"
                style={{
                  background:  'var(--bg-overlay)',
                  borderColor: 'var(--border-default)',
                  color:       'var(--text-primary)',
                }}
                aria-label="Custom date from"
              />
              <Input
                type="date"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
                placeholder="To"
                className="h-8 text-xs"
                style={{
                  background:  'var(--bg-overlay)',
                  borderColor: 'var(--border-default)',
                  color:       'var(--text-primary)',
                }}
                aria-label="Custom date to"
              />
              <Button
                size="sm"
                className="w-full h-7 text-xs"
                style={{ background: 'var(--accent-600)', color: '#fff' }}
                onClick={handleCustomApply}
              >
                Apply
              </Button>
            </div>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  )
}
