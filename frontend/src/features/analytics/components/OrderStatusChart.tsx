/**
 * features/analytics/components/OrderStatusChart.tsx
 *
 * Bar chart showing order counts by status + completion/cancellation rate cards.
 */
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell,
} from 'recharts'
import { ShoppingCart, AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useOrderAnalytics } from '../hooks/useAnalytics'
import { formatNumber, formatPercent } from '../utils/dateUtils'
import type { AnalyticsDateRange, OrderAnalyticsResponse } from '../types/analytics.types'

interface Props {
  range: AnalyticsDateRange
}

const STATUS_CONFIG: {
  key: keyof OrderAnalyticsResponse
  label: string
  color: string
}[] = [
  { key: 'pendingOrders',    label: 'Pending',    color: '#f59e0b' },
  { key: 'confirmedOrders',  label: 'Confirmed',  color: '#38bdf8' },
  { key: 'processingOrders', label: 'Processing', color: '#818cf8' },
  { key: 'shippedOrders',    label: 'Shipped',    color: '#a78bfa' },
  { key: 'deliveredOrders',  label: 'Delivered',  color: '#22d3ee' },
  { key: 'completedOrders',  label: 'Completed',  color: '#22c55e' },
  { key: 'cancelledOrders',  label: 'Cancelled',  color: '#ef4444' },
]

function CustomTooltip({ active, payload, label }: any) {
  if (!active || !payload?.length) return null
  return (
    <div
      className="rounded-lg p-3 shadow-xl text-sm"
      style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-strong)', color: 'var(--text-primary)' }}
    >
      <p className="font-semibold mb-1" style={{ color: 'var(--text-secondary)' }}>{label}</p>
      <p style={{ color: payload[0]?.fill }}>{formatNumber(payload[0]?.value ?? 0)} orders</p>
    </div>
  )
}

export function OrderStatusChart({ range }: Props) {
  const { data, isLoading, isError, refetch } = useOrderAnalytics(range)

  const chartData = data
    ? STATUS_CONFIG.map((s) => ({
        label:  s.label,
        count:  (data[s.key] as number) ?? 0,
        color:  s.color,
      }))
    : []

  return (
    <div
      className="rounded-xl p-5"
      style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)' }}
    >
      {/* Header */}
      <div className="flex items-center gap-2 mb-4">
        <ShoppingCart className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
        <h2 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
          Order Status Breakdown
        </h2>
      </div>

      {/* Rate pills */}
      {data && (
        <div className="flex gap-3 mb-4 flex-wrap">
          <div
            className="rounded-lg px-3 py-1.5 text-xs font-medium"
            style={{ background: 'rgba(34,197,94,0.1)', color: '#22c55e' }}
          >
            Completion Rate: {formatPercent(data.completionRate)}
          </div>
          <div
            className="rounded-lg px-3 py-1.5 text-xs font-medium"
            style={{ background: 'rgba(239,68,68,0.1)', color: '#ef4444' }}
          >
            Cancellation Rate: {formatPercent(data.cancellationRate)}
          </div>
          <div
            className="rounded-lg px-3 py-1.5 text-xs font-medium"
            style={{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }}
          >
            Total: {formatNumber(data.totalOrders)} orders
          </div>
        </div>
      )}

      {/* Loading */}
      {isLoading && (
        <div className="h-56 animate-pulse rounded-xl" style={{ background: 'var(--bg-elevated)' }} aria-label="Loading..." />
      )}

      {/* Error */}
      {isError && (
        <div className="h-56 flex flex-col items-center justify-center gap-3">
          <AlertTriangle className="h-8 w-8" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>Unable to load order data.</p>
          <Button size="sm" variant="outline" onClick={() => refetch()}
            style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}>
            Retry
          </Button>
        </div>
      )}

      {/* Chart */}
      {!isLoading && !isError && (
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={chartData} margin={{ top: 5, right: 10, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border-subtle)" vertical={false} />
            <XAxis dataKey="label" tick={{ fontSize: 11, fill: 'var(--text-muted)' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: 'var(--text-muted)' }} axisLine={false} tickLine={false} />
            <Tooltip content={<CustomTooltip />} />
            <Bar dataKey="count" radius={[4, 4, 0, 0]} maxBarSize={40}>
              {chartData.map((entry) => (
                <Cell key={entry.label} fill={entry.color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
