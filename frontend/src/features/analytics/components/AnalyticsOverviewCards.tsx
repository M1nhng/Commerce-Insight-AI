/**
 * features/analytics/components/AnalyticsOverviewCards.tsx
 *
 * KPI cards grid for the analytics dashboard overview.
 * Reads from useAnalyticsOverview() and displays 7 metrics.
 */
import {
  DollarSign, ShoppingCart, Users, Package,
  TrendingUp, XCircle, AlertTriangle,
} from 'lucide-react'
import { useAnalyticsOverview } from '../hooks/useAnalytics'
import { formatCurrency, formatNumber, formatPercent } from '../utils/dateUtils'
import type { AnalyticsDateRange } from '../types/analytics.types'

interface Props {
  range: AnalyticsDateRange
}

interface KpiCardProps {
  label:     string
  value:     string
  icon:      React.ElementType
  iconColor: string
  iconBg:    string
  loading:   boolean
}

function KpiCard({ label, value, icon: Icon, iconColor, iconBg, loading }: KpiCardProps) {
  return (
    <div
      className="rounded-xl p-5 flex items-start gap-4 transition-all duration-200 hover:scale-[1.01]"
      style={{
        background:   'var(--bg-surface)',
        border:       '1px solid var(--border-default)',
        boxShadow:    '0 1px 3px rgba(0,0,0,0.3)',
      }}
      role="article"
    >
      <div
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg"
        style={{ background: iconBg }}
      >
        <Icon className="h-5 w-5" style={{ color: iconColor }} />
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium truncate mb-1" style={{ color: 'var(--text-muted)' }}>
          {label}
        </p>
        {loading ? (
          <div
            className="h-7 w-28 animate-pulse rounded-md"
            style={{ background: 'var(--bg-elevated)' }}
            aria-label="Loading..."
          />
        ) : (
          <p className="text-xl font-bold truncate" style={{ color: 'var(--text-primary)' }}>
            {value}
          </p>
        )}
      </div>
    </div>
  )
}

export function AnalyticsOverviewCards({ range }: Props) {
  const { data, isLoading, isError } = useAnalyticsOverview(range)

  if (isError) {
    return (
      <div
        className="rounded-xl p-6 text-center"
        style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)' }}
      >
        <AlertTriangle className="h-8 w-8 mx-auto mb-2" style={{ color: 'var(--error)' }} />
        <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
          Unable to load overview data.
        </p>
      </div>
    )
  }

  const currency = data?.currency ?? 'VND'

  const cards: Omit<KpiCardProps, 'loading'>[] = [
    {
      label:     'Total Revenue',
      value:     data ? formatCurrency(data.totalRevenue, currency) : '—',
      icon:      DollarSign,
      iconColor: '#22d3ee',
      iconBg:    'rgba(34,211,238,0.12)',
    },
    {
      label:     'Total Orders',
      value:     data ? formatNumber(data.totalOrders) : '—',
      icon:      ShoppingCart,
      iconColor: '#818cf8',
      iconBg:    'rgba(129,140,248,0.12)',
    },
    {
      label:     'Unique Customers',
      value:     data ? formatNumber(data.totalCustomers) : '—',
      icon:      Users,
      iconColor: '#34d399',
      iconBg:    'rgba(52,211,153,0.12)',
    },
    {
      label:     'Products Sold',
      value:     data ? formatNumber(data.totalProductsSold) : '—',
      icon:      Package,
      iconColor: '#a78bfa',
      iconBg:    'rgba(167,139,250,0.12)',
    },
    {
      label:     'Avg. Order Value',
      value:     data ? formatCurrency(data.averageOrderValue, currency) : '—',
      icon:      TrendingUp,
      iconColor: '#fb923c',
      iconBg:    'rgba(251,146,60,0.12)',
    },
    {
      label:     'Cancelled Orders',
      value:     data ? formatNumber(data.cancelledOrders) : '—',
      icon:      XCircle,
      iconColor: '#ef4444',
      iconBg:    'rgba(239,68,68,0.12)',
    },
    {
      label:     'Cancellation Rate',
      value:     data ? formatPercent(data.cancellationRate) : '—',
      icon:      AlertTriangle,
      iconColor: '#f59e0b',
      iconBg:    'rgba(245,158,11,0.12)',
    },
  ]

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-7">
      {cards.map((card) => (
        <KpiCard key={card.label} {...card} loading={isLoading} />
      ))}
    </div>
  )
}
