/**
 * features/analytics/components/RevenueTrendChart.tsx
 *
 * Revenue time-series area chart using Recharts.
 * Supports DAY / WEEK / MONTH grouping with a toggle.
 */
import { useState } from 'react'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { TrendingUp, AlertTriangle, BarChart2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useRevenueAnalytics } from '../hooks/useAnalytics'
import { formatCurrency, formatNumber } from '../utils/dateUtils'
import type { AnalyticsDateRange, RevenueGroupBy } from '../types/analytics.types'

interface Props {
  range: AnalyticsDateRange
}

const GROUP_BY_OPTIONS: { label: string; value: RevenueGroupBy }[] = [
  { label: 'Day',   value: 'DAY' },
  { label: 'Week',  value: 'WEEK' },
  { label: 'Month', value: 'MONTH' },
]

// Tooltip component styled for dark theme
function CustomTooltip({ active, payload, label, currency }: any) {
  if (!active || !payload?.length) return null
  return (
    <div
      className="rounded-lg p-3 shadow-xl text-sm"
      style={{
        background:  'var(--bg-elevated)',
        border:      '1px solid var(--border-strong)',
        color:       'var(--text-primary)',
      }}
    >
      <p className="font-semibold mb-1" style={{ color: 'var(--text-secondary)' }}>{label}</p>
      {payload.map((entry: any) => (
        <p key={entry.dataKey} style={{ color: entry.color }}>
          {entry.dataKey === 'revenue'
            ? `Revenue: ${formatCurrency(entry.value, currency ?? 'VND')}`
            : `Orders: ${formatNumber(entry.value)}`}
        </p>
      ))}
    </div>
  )
}

export function RevenueTrendChart({ range }: Props) {
  const [groupBy, setGroupBy] = useState<RevenueGroupBy>('DAY')
  const { data, isLoading, isError, refetch } = useRevenueAnalytics(range, groupBy)
  const currency = data?.currency ?? 'VND'
  const chartData = data?.data ?? []

  return (
    <div
      className="rounded-xl p-5"
      style={{
        background: 'var(--bg-surface)',
        border:     '1px solid var(--border-default)',
      }}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
        <div className="flex items-center gap-2">
          <TrendingUp className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
          <h2 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
            Revenue Trend
          </h2>
        </div>

        {/* GroupBy toggle */}
        <div
          className="flex items-center rounded-lg p-0.5 gap-0.5"
          style={{ background: 'var(--bg-elevated)' }}
          role="group"
          aria-label="Revenue grouping"
        >
          {GROUP_BY_OPTIONS.map(({ label, value }) => (
            <Button
              key={value}
              size="sm"
              variant="ghost"
              onClick={() => setGroupBy(value)}
              className="h-7 px-3 text-xs rounded-md transition-all"
              style={
                groupBy === value
                  ? { background: 'var(--accent-600)', color: '#fff' }
                  : { color: 'var(--text-secondary)' }
              }
              aria-pressed={groupBy === value}
            >
              {label}
            </Button>
          ))}
        </div>
      </div>

      {/* Chart body */}
      {isLoading && (
        <div
          className="h-64 animate-pulse rounded-xl"
          style={{ background: 'var(--bg-elevated)' }}
          aria-label="Loading chart..."
        />
      )}

      {isError && (
        <div className="h-64 flex flex-col items-center justify-center gap-3">
          <AlertTriangle className="h-8 w-8" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            Unable to load revenue data.
          </p>
          <Button size="sm" variant="outline" onClick={() => refetch()}
            style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}>
            Retry
          </Button>
        </div>
      )}

      {!isLoading && !isError && chartData.length === 0 && (
        <div className="h-64 flex flex-col items-center justify-center gap-2">
          <BarChart2 className="h-10 w-10" style={{ color: 'var(--text-muted)' }} />
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            No revenue data for this period.
          </p>
        </div>
      )}

      {!isLoading && !isError && chartData.length > 0 && (
        <ResponsiveContainer width="100%" height={280}>
          <AreaChart data={chartData} margin={{ top: 5, right: 10, left: 10, bottom: 0 }}>
            <defs>
              <linearGradient id="gradRevenue" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="var(--chart-1)" stopOpacity={0.3} />
                <stop offset="95%" stopColor="var(--chart-1)" stopOpacity={0.02} />
              </linearGradient>
              <linearGradient id="gradOrders" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor="var(--chart-2)" stopOpacity={0.2} />
                <stop offset="95%" stopColor="var(--chart-2)" stopOpacity={0.02} />
              </linearGradient>
            </defs>

            <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" vertical={false} />

            <XAxis
              dataKey="period"
              tick={{ fontSize: 11, fill: 'var(--text-muted)' }}
              axisLine={false}
              tickLine={false}
              interval="preserveStartEnd"
            />

            <YAxis
              yAxisId="revenue"
              orientation="left"
              tick={{ fontSize: 11, fill: 'var(--text-muted)' }}
              axisLine={false}
              tickLine={false}
              tickFormatter={(v) => formatCurrency(v, currency).replace(/\s/g, '')}
              width={90}
            />

            <YAxis
              yAxisId="orders"
              orientation="right"
              tick={{ fontSize: 11, fill: 'var(--text-muted)' }}
              axisLine={false}
              tickLine={false}
              width={40}
            />

            <Tooltip content={<CustomTooltip currency={currency} />} />
            <Legend
              wrapperStyle={{ fontSize: 12, color: 'var(--text-secondary)', paddingTop: 8 }}
            />

            <Area
              yAxisId="revenue"
              type="monotone"
              dataKey="revenue"
              name="Revenue"
              stroke="var(--chart-1)"
              strokeWidth={2}
              fill="url(#gradRevenue)"
              dot={false}
              activeDot={{ r: 4, strokeWidth: 0, fill: 'var(--chart-1)' }}
            />
            <Area
              yAxisId="orders"
              type="monotone"
              dataKey="orders"
              name="Orders"
              stroke="var(--chart-2)"
              strokeWidth={2}
              fill="url(#gradOrders)"
              dot={false}
              activeDot={{ r: 4, strokeWidth: 0, fill: 'var(--chart-2)' }}
            />
          </AreaChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
