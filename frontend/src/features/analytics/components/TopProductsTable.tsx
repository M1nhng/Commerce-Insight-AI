/**
 * features/analytics/components/TopProductsTable.tsx
 *
 * Table showing top N products by revenue.
 * Allows switching between Top 5 / Top 10 / Top 20.
 */
import { useState } from 'react'
import { Package, AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useTopProducts } from '../hooks/useAnalytics'
import { formatCurrency, formatNumber } from '../utils/dateUtils'
import type { AnalyticsDateRange } from '../types/analytics.types'

interface Props {
  range:    AnalyticsDateRange
  currency?: string
}

const LIMIT_OPTIONS = [5, 10, 20]

export function TopProductsTable({ range, currency = 'VND' }: Props) {
  const [limit, setLimit] = useState(10)
  const { data, isLoading, isError, refetch } = useTopProducts(range, limit)
  const products = data ?? []

  return (
    <div
      className="rounded-xl p-5"
      style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)' }}
    >
      {/* Header */}
      <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
        <div className="flex items-center gap-2">
          <Package className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
          <h2 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
            Top Products by Revenue
          </h2>
        </div>

        {/* Limit toggle */}
        <div
          className="flex items-center rounded-lg p-0.5 gap-0.5"
          style={{ background: 'var(--bg-elevated)' }}
          role="group"
          aria-label="Top products count"
        >
          {LIMIT_OPTIONS.map((l) => (
            <Button
              key={l}
              size="sm"
              variant="ghost"
              onClick={() => setLimit(l)}
              className="h-7 px-3 text-xs rounded-md"
              style={
                limit === l
                  ? { background: 'var(--accent-600)', color: '#fff' }
                  : { color: 'var(--text-secondary)' }
              }
              aria-pressed={limit === l}
            >
              Top {l}
            </Button>
          ))}
        </div>
      </div>

      {/* Loading skeletons */}
      {isLoading && (
        <div className="space-y-3">
          {Array.from({ length: limit > 5 ? 5 : limit }).map((_, i) => (
            <div key={i} className="flex items-center gap-3">
              <div className="h-8 w-8 animate-pulse rounded-lg" style={{ background: 'var(--bg-elevated)' }} />
              <div className="flex-1 h-4 animate-pulse rounded" style={{ background: 'var(--bg-elevated)' }} />
              <div className="h-4 w-20 animate-pulse rounded" style={{ background: 'var(--bg-elevated)' }} />
            </div>
          ))}
        </div>
      )}

      {/* Error */}
      {isError && (
        <div className="flex flex-col items-center justify-center py-8 gap-3">
          <AlertTriangle className="h-8 w-8" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>Unable to load product data.</p>
          <Button size="sm" variant="outline" onClick={() => refetch()}
            style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}>
            Retry
          </Button>
        </div>
      )}

      {/* Empty */}
      {!isLoading && !isError && products.length === 0 && (
        <div className="flex flex-col items-center justify-center py-8 gap-2">
          <Package className="h-10 w-10" style={{ color: 'var(--text-muted)' }} />
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>No product sales in this period.</p>
        </div>
      )}

      {/* Table */}
      {!isLoading && !isError && products.length > 0 && (
        <div className="overflow-x-auto -mx-5 px-5">
          <table className="w-full text-sm min-w-[520px]">
            <thead>
              <tr style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                {['#', 'Product', 'SKU', 'Qty Sold', 'Revenue'].map((h) => (
                  <th
                    key={h}
                    className="pb-2 pt-1 text-left text-xs font-medium"
                    style={{ color: 'var(--text-muted)' }}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {products.map((p, idx) => (
                <tr
                  key={p.productId ?? p.sku}
                  className="transition-colors hover:opacity-80"
                  style={{ borderBottom: '1px solid var(--border-subtle)' }}
                >
                  {/* Rank */}
                  <td className="py-2.5 pr-3 w-8">
                    <span
                      className="inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-semibold"
                      style={
                        idx === 0
                          ? { background: 'rgba(251,191,36,0.15)', color: '#fbbf24' }
                          : idx === 1
                          ? { background: 'rgba(148,163,184,0.1)', color: '#94a3b8' }
                          : idx === 2
                          ? { background: 'rgba(180,120,60,0.1)', color: '#b47c3c' }
                          : { background: 'var(--bg-elevated)', color: 'var(--text-muted)' }
                      }
                    >
                      {idx + 1}
                    </span>
                  </td>

                  {/* Product name */}
                  <td className="py-2.5 pr-4 max-w-[200px]">
                    <p className="truncate font-medium" style={{ color: 'var(--text-primary)' }}>
                      {p.productName}
                    </p>
                  </td>

                  {/* SKU */}
                  <td className="py-2.5 pr-4">
                    <span
                      className="rounded px-1.5 py-0.5 text-xs font-mono"
                      style={{ background: 'var(--bg-elevated)', color: 'var(--text-secondary)' }}
                    >
                      {p.sku}
                    </span>
                  </td>

                  {/* Qty */}
                  <td className="py-2.5 pr-4">
                    <span style={{ color: 'var(--text-secondary)' }}>{formatNumber(p.quantitySold)}</span>
                  </td>

                  {/* Revenue */}
                  <td className="py-2.5 text-right">
                    <span className="font-semibold" style={{ color: 'var(--text-primary)' }}>
                      {formatCurrency(p.revenue, currency)}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
