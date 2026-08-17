/**
 * features/orders/pages/OrdersPage.tsx
 *
 * Main order list page with search, filters, pagination, and quick cancel.
 * Pattern mirrors CustomersPage.tsx exactly.
 */
import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Search, X, SlidersHorizontal, ShoppingCart } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { OrderTable } from '../components/OrderTable'
import { useOrders, useCancelOrder } from '../hooks/useOrders'
import { useAuth } from '@/hooks/useAuth'
import type {
  OrderSummaryResponse,
  OrderFilterParams,
  OrderStatus,
  PaymentStatus,
} from '@/types/order.types'

// ── Pagination (copied from CustomersPage pattern) ─────────────────────────

function Pagination({
  page, totalPages, totalElements, size, onPageChange, onSizeChange,
}: {
  page: number; totalPages: number; totalElements: number
  size: number; onPageChange: (p: number) => void; onSizeChange: (s: number) => void
}) {
  const start = page * size + 1
  const end   = Math.min((page + 1) * size, totalElements)

  return (
    <div className="flex items-center justify-between mt-4 flex-wrap gap-3">
      <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
        Showing{' '}
        <strong style={{ color: 'var(--text-primary)' }}>
          {totalElements === 0 ? 0 : start}–{end}
        </strong>{' '}
        of <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> orders
      </p>

      <div className="flex items-center gap-3">
        <Select value={String(size)} onValueChange={(v) => { onSizeChange(Number(v)); onPageChange(0) }}>
          <SelectTrigger
            className="w-28 h-8 text-body-sm"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            {[10, 25, 50].map((s) => (
              <SelectItem key={s} value={String(s)} style={{ color: 'var(--text-primary)' }}>{s} / page</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-1">
          <Button
            variant="outline" size="sm"
            onClick={() => onPageChange(page - 1)} disabled={page === 0}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >← Prev</Button>

          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
            const p = totalPages <= 5 ? i : Math.max(0, Math.min(page - 2, totalPages - 5)) + i
            return (
              <Button
                key={p} variant={p === page ? 'default' : 'outline'} size="sm"
                onClick={() => onPageChange(p)}
                style={p === page
                  ? { background: 'var(--accent-500)', color: '#fff', border: 'none' }
                  : { background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
              >{p + 1}</Button>
            )
          })}

          <Button
            variant="outline" size="sm"
            onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >Next →</Button>
        </div>
      </div>
    </div>
  )
}

// ── Status tab pills (from §12.4 spec) ───────────────────────────────────

const STATUS_TABS: Array<{ label: string; value: OrderStatus | '__all__' }> = [
  { label: 'All',        value: '__all__'    },
  { label: 'Pending',    value: 'PENDING'    },
  { label: 'Confirmed',  value: 'CONFIRMED'  },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Shipped',    value: 'SHIPPED'    },
  { label: 'Delivered',  value: 'DELIVERED'  },
  { label: 'Cancelled',  value: 'CANCELLED'  },
]

// ── Main Page ─────────────────────────────────────────────────────────────

export function OrdersPage() {
  const navigate    = useNavigate()
  const { isAtLeast } = useAuth()
  const canWrite    = isAtLeast('MANAGER')

  // ── Filter state ─────────────────────────────────────────────────────
  const [search, setSearch]             = useState('')
  const [debouncedSearch, setDebounced] = useState('')
  const [statusTab, setStatusTab]       = useState<OrderStatus | '__all__'>('__all__')
  const [paymentFilter, setPaymentFilter] = useState<string>('__all__')
  const [page, setPage]                 = useState(0)
  const [size, setSize]                 = useState(10)

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value)
    const t = setTimeout(() => { setDebounced(value); setPage(0) }, 400)
    return () => clearTimeout(t)
  }, [])

  const filters: OrderFilterParams = {
    keyword:       debouncedSearch || undefined,
    status:        statusTab !== '__all__' ? statusTab : undefined,
    paymentStatus: paymentFilter !== '__all__' ? (paymentFilter as PaymentStatus) : undefined,
    page,
    size,
    sort:          'createdAt,desc',
  }

  // ── Data ─────────────────────────────────────────────────────────────
  const { data, isLoading } = useOrders(filters)

  // ── Cancel mutation ───────────────────────────────────────────────────
  const cancelOrder = useCancelOrder()
  const [cancelTarget, setCancelTarget] = useState<OrderSummaryResponse | null>(null)
  const [cancelReason, setCancelReason] = useState('')

  const handleConfirmCancel = () => {
    if (!cancelTarget) return
    cancelOrder.mutate(
      { id: cancelTarget.id, data: { reason: cancelReason || undefined } },
      { onSuccess: () => { setCancelTarget(null); setCancelReason('') } }
    )
  }

  const clearFilters = () => {
    setSearch(''); setDebounced(''); setStatusTab('__all__'); setPaymentFilter('__all__'); setPage(0)
  }

  const hasFilters = debouncedSearch || statusTab !== '__all__' || paymentFilter !== '__all__'

  return (
    <div className="animate-fade-in space-y-6">

      {/* ── Page Header ──────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-1 flex items-center gap-2" style={{ color: 'var(--text-primary)' }}>
            <ShoppingCart className="h-7 w-7" style={{ color: 'var(--accent-400)' }} />
            Orders
          </h1>
          <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
            {data?.totalElements ?? 0} orders in your database
          </p>
        </div>

        {canWrite && (
          <Button
            onClick={() => navigate('/orders/new')}
            className="gap-2"
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            <Plus className="h-4 w-4" /> New Order
          </Button>
        )}
      </div>

      {/* ── Status Tabs ───────────────────────────────────────────────────── */}
      <div
        className="flex items-center gap-1 flex-wrap p-1 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        {STATUS_TABS.map((tab) => {
          const active = tab.value === statusTab
          return (
            <button
              key={tab.value}
              onClick={() => { setStatusTab(tab.value); setPage(0) }}
              className="px-3 py-1.5 rounded-lg text-body-sm font-medium transition-all"
              style={active
                ? { background: 'var(--accent-500)', color: '#fff' }
                : { color: 'var(--text-secondary)', background: 'transparent' }
              }
            >
              {tab.label}
            </button>
          )
        })}
      </div>

      {/* ── Filter Bar ────────────────────────────────────────────────────── */}
      <div
        className="flex flex-wrap gap-3 p-4 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4"
            style={{ color: 'var(--text-muted)' }}
          />
          <Input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by order number..."
            className="pl-9"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
          />
        </div>

        {/* Payment Status filter */}
        <Select value={paymentFilter} onValueChange={(v) => { setPaymentFilter(v); setPage(0) }}>
          <SelectTrigger
            className="w-44"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SlidersHorizontal className="h-3.5 w-3.5 mr-2 shrink-0" style={{ color: 'var(--text-muted)' }} />
            <SelectValue placeholder="Payment Status" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value="__all__" style={{ color: 'var(--text-secondary)' }}>All Payments</SelectItem>
            <SelectItem value="PENDING"  style={{ color: 'var(--warning)' }}>Pending</SelectItem>
            <SelectItem value="PAID"     style={{ color: 'var(--success)' }}>Paid</SelectItem>
            <SelectItem value="FAILED"   style={{ color: 'var(--error)' }}>Failed</SelectItem>
            <SelectItem value="REFUNDED" style={{ color: 'var(--text-muted)' }}>Refunded</SelectItem>
          </SelectContent>
        </Select>

        {hasFilters && (
          <Button
            variant="ghost" size="sm" onClick={clearFilters}
            className="gap-1.5" style={{ color: 'var(--text-muted)' }}
          >
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}
      </div>

      {/* ── Table ─────────────────────────────────────────────────────────── */}
      <OrderTable
        orders={data?.content ?? []}
        isLoading={isLoading}
        canWrite={canWrite}
        onCancel={setCancelTarget}
      />

      {/* ── Pagination ────────────────────────────────────────────────────── */}
      {data && data.totalPages > 0 && (
        <Pagination
          page={page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={setSize}
        />
      )}

      {/* ── Cancel Dialog ─────────────────────────────────────────────────── */}
      <ConfirmDialog
        open={!!cancelTarget}
        onOpenChange={(o) => !o && setCancelTarget(null)}
        title="Cancel Order?"
        description={`Cancel order "${cancelTarget?.orderNumber}"? This will release all inventory reservations and cannot be undone.`}
        confirmLabel="Cancel Order"
        variant="destructive"
        loading={cancelOrder.isPending}
        onConfirm={handleConfirmCancel}
      />
    </div>
  )
}
