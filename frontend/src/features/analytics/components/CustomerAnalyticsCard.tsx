/**
 * features/analytics/components/CustomerAnalyticsCard.tsx
 *
 * Compact card section displaying customer engagement metrics.
 */
import { Users, UserPlus, Repeat2, BarChart2, AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useCustomerAnalytics } from '../hooks/useAnalytics'
import { formatNumber } from '../utils/dateUtils'
import type { AnalyticsDateRange } from '../types/analytics.types'

interface Props {
  range: AnalyticsDateRange
}

interface MetricRowProps {
  icon:    React.ElementType
  color:   string
  label:   string
  value:   string
  loading: boolean
}

function MetricRow({ icon: Icon, color, label, value, loading }: MetricRowProps) {
  return (
    <div className="flex items-center gap-3 py-2.5">
      <div
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg"
        style={{ background: `${color}18` }}
      >
        <Icon className="h-4 w-4" style={{ color }} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs" style={{ color: 'var(--text-muted)' }}>{label}</p>
        {loading ? (
          <div className="h-5 w-20 animate-pulse rounded" style={{ background: 'var(--bg-elevated)' }} />
        ) : (
          <p className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>{value}</p>
        )}
      </div>
    </div>
  )
}

export function CustomerAnalyticsCard({ range }: Props) {
  const { data, isLoading, isError, refetch } = useCustomerAnalytics(range)

  const metrics = [
    {
      icon: Users, color: '#818cf8', label: 'Unique Customers',
      value: data ? formatNumber(data.uniqueCustomers) : '—',
    },
    {
      icon: UserPlus, color: '#34d399', label: 'New Customers',
      value: data ? formatNumber(data.newCustomers) : '—',
    },
    {
      icon: Repeat2, color: '#22d3ee', label: 'Repeat Customers',
      value: data ? formatNumber(data.repeatCustomers) : '—',
    },
    {
      icon: BarChart2, color: '#fb923c', label: 'Avg. Orders / Customer',
      value: data ? data.averageOrdersPerCustomer.toFixed(2) : '—',
    },
  ]

  return (
    <div
      className="rounded-xl p-5"
      style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)' }}
    >
      {/* Header */}
      <div className="flex items-center gap-2 mb-2">
        <Users className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
        <h2 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
          Customer Analytics
        </h2>
      </div>

      {isError ? (
        <div className="flex flex-col items-center justify-center py-8 gap-3">
          <AlertTriangle className="h-8 w-8" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>Unable to load customer data.</p>
          <Button size="sm" variant="outline" onClick={() => refetch()}
            style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}>
            Retry
          </Button>
        </div>
      ) : (
        <div className="divide-y" style={{ borderColor: 'var(--border-subtle)' }}>
          {metrics.map((m) => (
            <MetricRow key={m.label} {...m} loading={isLoading} />
          ))}
        </div>
      )}
    </div>
  )
}
