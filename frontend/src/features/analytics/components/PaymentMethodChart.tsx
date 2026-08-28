/**
 * features/analytics/components/PaymentMethodChart.tsx
 *
 * Pie chart showing payment method breakdown by order count and amount.
 */
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import { CreditCard, AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { usePaymentAnalytics } from '../hooks/useAnalytics'
import { formatCurrency, formatNumber, PAYMENT_METHOD_LABELS } from '../utils/dateUtils'
import type { AnalyticsDateRange } from '../types/analytics.types'

interface Props {
  range: AnalyticsDateRange
}

const METHOD_COLORS: Record<string, string> = {
  CASH:          '#34d399',
  BANK_TRANSFER: '#818cf8',
  CARD:          '#22d3ee',
  OTHER:         '#fb923c',
}

function CustomTooltip({ active, payload, currency }: any) {
  if (!active || !payload?.length) return null
  const d = payload[0]
  return (
    <div
      className="rounded-lg p-3 shadow-xl text-sm"
      style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-strong)', color: 'var(--text-primary)' }}
    >
      <p className="font-semibold mb-1" style={{ color: d.payload.fill }}>{d.name}</p>
      <p style={{ color: 'var(--text-secondary)' }}>{formatNumber(d.payload.orders)} orders</p>
      <p style={{ color: 'var(--text-secondary)' }}>{formatCurrency(d.payload.amount, currency)}</p>
    </div>
  )
}

export function PaymentMethodChart({ range }: Props) {
  const { data, isLoading, isError, refetch } = usePaymentAnalytics(range)
  const currency = data?.currency ?? 'VND'

  const chartData = data
    ? Object.entries(data.breakdown).map(([method, stats]) => ({
        name:   PAYMENT_METHOD_LABELS[method] ?? method,
        orders: stats.orders,
        amount: stats.amount,
        fill:   METHOD_COLORS[method] ?? '#8888a4',
      }))
    : []

  return (
    <div
      className="rounded-xl p-5"
      style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)' }}
    >
      {/* Header */}
      <div className="flex items-center gap-2 mb-4">
        <CreditCard className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
        <h2 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
          Payment Methods
        </h2>
      </div>

      {isLoading && (
        <div className="h-56 animate-pulse rounded-xl" style={{ background: 'var(--bg-elevated)' }} aria-label="Loading..." />
      )}

      {isError && (
        <div className="h-56 flex flex-col items-center justify-center gap-3">
          <AlertTriangle className="h-8 w-8" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>Unable to load payment data.</p>
          <Button size="sm" variant="outline" onClick={() => refetch()}
            style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}>
            Retry
          </Button>
        </div>
      )}

      {!isLoading && !isError && chartData.length === 0 && (
        <div className="h-56 flex items-center justify-center">
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>No payment data for this period.</p>
        </div>
      )}

      {!isLoading && !isError && chartData.length > 0 && (
        <>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={chartData}
                dataKey="orders"
                nameKey="name"
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                paddingAngle={3}
              >
                {chartData.map((entry) => (
                  <Cell key={entry.name} fill={entry.fill} stroke="transparent" />
                ))}
              </Pie>
              <Tooltip content={<CustomTooltip currency={currency} />} />
              <Legend
                wrapperStyle={{ fontSize: 12, color: 'var(--text-secondary)', paddingTop: 8 }}
              />
            </PieChart>
          </ResponsiveContainer>

          {/* Summary table */}
          <div className="mt-3 space-y-1.5">
            {chartData.map((entry) => (
              <div key={entry.name} className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <span className="h-2 w-2 rounded-full shrink-0" style={{ background: entry.fill }} aria-hidden />
                  <span style={{ color: 'var(--text-secondary)' }}>{entry.name}</span>
                </div>
                <div className="flex items-center gap-4">
                  <span style={{ color: 'var(--text-muted)' }}>{formatNumber(entry.orders)} orders</span>
                  <span className="font-medium" style={{ color: 'var(--text-primary)' }}>
                    {formatCurrency(entry.amount, currency)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  )
}
