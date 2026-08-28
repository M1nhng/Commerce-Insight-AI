/**
 * features/analytics/pages/AnalyticsPage.tsx
 *
 * Main analytics dashboard page.
 *
 * Layout:
 *   - Page header with title + date filter
 *   - Overview KPI cards (full width)
 *   - Revenue trend chart (full width)
 *   - 2-column: Order status chart | Payment method chart
 *   - 2-column: Customer analytics card | Top products table
 *
 * Architecture:
 *   - All queries use the same `range` object so date changes
 *     propagate consistently to every chart/card.
 *   - Individual query failures show per-section errors, not a blank page.
 *   - No mock data — all data from the real Spring Boot API.
 */
import { useState } from 'react'
import { BarChart3 } from 'lucide-react'
import { AnalyticsDateFilter }    from '../components/AnalyticsDateFilter'
import { AnalyticsOverviewCards } from '../components/AnalyticsOverviewCards'
import { RevenueTrendChart }      from '../components/RevenueTrendChart'
import { OrderStatusChart }       from '../components/OrderStatusChart'
import { PaymentMethodChart }     from '../components/PaymentMethodChart'
import { CustomerAnalyticsCard }  from '../components/CustomerAnalyticsCard'
import { TopProductsTable }       from '../components/TopProductsTable'
import { getDateRange }           from '../utils/dateUtils'
import type { AnalyticsDateRange } from '../types/analytics.types'

// Default to Last 30 Days
const DEFAULT_RANGE: AnalyticsDateRange = (() => {
  const r = getDateRange('last30days')
  return { dateFrom: r.dateFrom, dateTo: r.dateTo }
})()

export function AnalyticsPage() {
  const [range, setRange] = useState<AnalyticsDateRange>(DEFAULT_RANGE)

  return (
    <div className="space-y-6">
      {/* ── Page Header ─────────────────────────────────────────────────── */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <BarChart3 className="h-6 w-6" style={{ color: 'var(--accent-400)' }} />
            <h1 className="text-2xl font-bold tracking-tight" style={{ color: 'var(--text-primary)' }}>
              Ecommerce Analytics
            </h1>
          </div>
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            Monitor sales, orders, customers and payment performance.
          </p>
        </div>

        <AnalyticsDateFilter value={range} onChange={setRange} />
      </div>

      {/* ── Overview KPI Cards ───────────────────────────────────────────── */}
      <AnalyticsOverviewCards range={range} />

      {/* ── Revenue Trend ────────────────────────────────────────────────── */}
      <RevenueTrendChart range={range} />

      {/* ── Order Status + Payment Methods ──────────────────────────────── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <OrderStatusChart    range={range} />
        <PaymentMethodChart  range={range} />
      </div>

      {/* ── Customer Analytics + Top Products ───────────────────────────── */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <CustomerAnalyticsCard range={range} />
        <div className="lg:col-span-2">
          <TopProductsTable range={range} />
        </div>
      </div>
    </div>
  )
}
