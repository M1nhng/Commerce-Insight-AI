/**
 * features/orders/pages/OrderDetailPage.tsx
 *
 * Full order detail view.
 * Layout: header + items table (left/main) + sidebar (right — status timeline, addresses, payment, actions)
 */
import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, Hash, User2, CreditCard, Truck, Clock, Loader2,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { CardSkeleton } from '@/components/common/TableSkeleton'
import { OrderStatusBadge } from '../components/OrderStatusBadge'
import { PaymentStatusBadge } from '../components/PaymentStatusBadge'
import { OrderStatusTimeline } from '../components/OrderStatusTimeline'
import { OrderItemsTable } from '../components/OrderItemsTable'
import { OrderAddressCard } from '../components/OrderAddressCard'
import { useOrder, useUpdateOrderStatus, useCancelOrder } from '../hooks/useOrders'
import { useAuth } from '@/hooks/useAuth'
import {
  ORDER_NEXT_ACTIONS,
  ORDER_ACTION_LABELS,
  PAYMENT_METHOD_LABELS,
} from '@/types/order.types'
import type { OrderStatus } from '@/types/order.types'

// ── Section Card wrapper ─────────────────────────────────────────────────

function SectionCard({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div
      className="rounded-xl border overflow-hidden"
      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
    >
      <div
        className="px-4 py-3 flex items-center gap-2 border-b"
        style={{ borderColor: 'var(--border-subtle)', background: 'var(--bg-elevated)' }}
      >
        <span style={{ color: 'var(--accent-400)' }}>{icon}</span>
        <h3 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>{title}</h3>
      </div>
      <div className="p-4">{children}</div>
    </div>
  )
}

// ── InfoRow ───────────────────────────────────────────────────────────────

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div
      className="flex items-start justify-between gap-4 py-2 border-b last:border-0"
      style={{ borderColor: 'var(--border-subtle)' }}
    >
      <span className="text-body-sm shrink-0" style={{ color: 'var(--text-muted)' }}>{label}</span>
      <span className="text-body-sm font-medium text-right" style={{ color: 'var(--text-primary)' }}>{value ?? '—'}</span>
    </div>
  )
}

// ── Main Component ────────────────────────────────────────────────────────

export function OrderDetailPage() {
  const { id }     = useParams<{ id: string }>()
  const navigate   = useNavigate()
  const { isAtLeast } = useAuth()
  const canWrite   = isAtLeast('MANAGER')

  const { data: order, isLoading } = useOrder(id ?? null)

  const updateStatus = useUpdateOrderStatus()
  const cancelOrder  = useCancelOrder()

  // Confirm dialog state (for status transitions + cancel)
  const [pendingAction, setPendingAction] = useState<{ type: 'transition'; status: OrderStatus } | { type: 'cancel' } | null>(null)
  const [actionReason, setActionReason]   = useState('')

  // ── Loading state ─────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full animate-pulse" style={{ background: 'var(--bg-elevated)' }} />
          <div className="h-8 w-48 rounded animate-pulse" style={{ background: 'var(--bg-elevated)' }} />
        </div>
        <CardSkeleton />
        <CardSkeleton />
      </div>
    )
  }

  if (!order) {
    return (
      <div className="flex flex-col items-center justify-center py-24">
        <div className="text-5xl mb-4">🔍</div>
        <h2 className="text-heading-2 mb-2" style={{ color: 'var(--text-primary)' }}>Order not found</h2>
        <Button onClick={() => navigate('/orders')} style={{ background: 'var(--accent-500)', color: '#fff' }}>
          Back to Orders
        </Button>
      </div>
    )
  }

  // ── Allowed next actions ──────────────────────────────────────────────
  const nextStatuses = ORDER_NEXT_ACTIONS[order.status] ?? []

  const handleConfirm = () => {
    if (!pendingAction) return
    if (pendingAction.type === 'cancel') {
      cancelOrder.mutate(
        { id: order.id, data: { reason: actionReason || undefined } },
        { onSuccess: () => { setPendingAction(null); setActionReason('') } }
      )
    } else {
      updateStatus.mutate(
        { id: order.id, data: { status: pendingAction.status, reason: actionReason || undefined } },
        { onSuccess: () => { setPendingAction(null); setActionReason('') } }
      )
    }
  }

  const isMutating = updateStatus.isPending || cancelOrder.isPending

  // ── Render ────────────────────────────────────────────────────────────
  return (
    <div className="animate-fade-in space-y-6">

      {/* ── Page Header ──────────────────────────────────────────────────── */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost" size="icon"
            onClick={() => navigate('/orders')}
            style={{ color: 'var(--text-muted)' }}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <div className="flex items-center gap-3 flex-wrap">
              <h1 className="text-heading-1 font-mono" style={{ color: 'var(--text-primary)' }}>
                {order.orderNumber}
              </h1>
              <OrderStatusBadge status={order.status} size="md" />
              <PaymentStatusBadge status={order.paymentStatus} />
            </div>
            <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
              Created{' '}
              {new Date(order.createdAt).toLocaleString('en-GB', {
                day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
              })}
            </p>
          </div>
        </div>

        {/* Action Buttons — only allowed next states */}
        {canWrite && nextStatuses.length > 0 && (
          <div className="flex items-center gap-2 flex-wrap">
            {nextStatuses.map((ns) => {
              const isCancel = ns === 'CANCELLED'
              return (
                <Button
                  key={ns}
                  size="sm"
                  onClick={() => setPendingAction(isCancel ? { type: 'cancel' } : { type: 'transition', status: ns })}
                  disabled={isMutating}
                  style={
                    isCancel
                      ? { background: 'var(--error-bg)', color: 'var(--error)', border: '1px solid var(--error)' }
                      : { background: 'var(--accent-500)', color: '#fff' }
                  }
                >
                  {isMutating && <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />}
                  {ORDER_ACTION_LABELS[ns] ?? ns}
                </Button>
              )
            })}
          </div>
        )}
      </div>

      {/* ── Main grid ─────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* LEFT — Order Items + Overview */}
        <div className="lg:col-span-2 space-y-6">

          {/* Order summary card */}
          <SectionCard title="Order Summary" icon={<Hash className="h-4 w-4" />}>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-0">
              <InfoRow label="Order Number" value={<span className="font-mono text-caption" style={{ color: 'var(--accent-400)' }}>{order.orderNumber}</span>} />
              <InfoRow label="Customer" value={order.customerName} />
              <InfoRow label="Customer Code" value={order.customerCode} />
              <InfoRow label="Currency" value={order.currency} />
              <InfoRow label="Items" value={`${order.items.length} item${order.items.length !== 1 ? 's' : ''}`} />
              {order.notes && <InfoRow label="Notes" value={order.notes} />}
              {order.shippedAt && <InfoRow label="Shipped At" value={new Date(order.shippedAt).toLocaleString('en-GB')} />}
              {order.deliveredAt && <InfoRow label="Delivered At" value={new Date(order.deliveredAt).toLocaleString('en-GB')} />}
              {order.cancelledAt && <InfoRow label="Cancelled At" value={new Date(order.cancelledAt).toLocaleString('en-GB')} />}
              {order.completedAt && <InfoRow label="Completed At" value={new Date(order.completedAt).toLocaleString('en-GB')} />}
            </div>
          </SectionCard>

          {/* Line Items */}
          <div>
            <h3 className="text-body-sm font-semibold mb-3" style={{ color: 'var(--text-primary)' }}>
              Line Items
            </h3>
            <OrderItemsTable order={order} />
          </div>
        </div>

        {/* RIGHT — Status Timeline + Addresses + Payment */}
        <div className="space-y-6">

          {/* Status Timeline */}
          <SectionCard title="Status History" icon={<Clock className="h-4 w-4" />}>
            <OrderStatusTimeline history={order.statusHistory ?? []} />
          </SectionCard>

          {/* Payment */}
          {order.payment && (
            <SectionCard title="Payment" icon={<CreditCard className="h-4 w-4" />}>
              <div>
                <InfoRow label="Method"    value={PAYMENT_METHOD_LABELS[order.payment.method]} />
                <InfoRow label="Status"    value={<PaymentStatusBadge status={order.payment.status} />} />
                <InfoRow label="Amount"    value={new Intl.NumberFormat('vi-VN', { style: 'currency', currency: order.currency === 'VND' ? 'VND' : order.currency, maximumFractionDigits: 0 }).format(order.payment.amount)} />
                {order.payment.paidAt && <InfoRow label="Paid At" value={new Date(order.payment.paidAt).toLocaleString('en-GB')} />}
                {order.payment.reference && <InfoRow label="Reference" value={order.payment.reference} />}
              </div>
            </SectionCard>
          )}

          {/* Addresses */}
          <SectionCard title="Addresses" icon={<Truck className="h-4 w-4" />}>
            <div className="space-y-3">
              <OrderAddressCard title="Shipping Address" address={order.shippingAddress} />
              <OrderAddressCard title="Billing Address"  address={order.billingAddress} />
            </div>
          </SectionCard>

          {/* Customer info shortcut */}
          <SectionCard title="Customer" icon={<User2 className="h-4 w-4" />}>
            <div>
              <InfoRow label="Name" value={order.customerName} />
              {order.customerCode && <InfoRow label="Code" value={<span className="font-mono text-caption" style={{ color: 'var(--accent-400)' }}>{order.customerCode}</span>} />}
            </div>
            {order.customerId && (
              <Button
                variant="outline" size="sm" className="mt-3 w-full"
                onClick={() => navigate(`/customers/${order.customerId}`)}
                style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
              >
                View Customer Profile →
              </Button>
            )}
          </SectionCard>
        </div>
      </div>

      {/* ── Confirm Dialog ────────────────────────────────────────────────── */}
      <ConfirmDialog
        open={!!pendingAction}
        onOpenChange={(o) => !o && setPendingAction(null)}
        title={
          pendingAction?.type === 'cancel'
            ? 'Cancel Order?'
            : `${ORDER_ACTION_LABELS[(pendingAction as any)?.status] ?? 'Update Status'}?`
        }
        description={
          pendingAction?.type === 'cancel'
            ? `Cancel order "${order.orderNumber}"? All inventory reservations will be released. This cannot be undone.`
            : `Transition order "${order.orderNumber}" to ${(pendingAction as any)?.status}. Confirm?`
        }
        confirmLabel={pendingAction?.type === 'cancel' ? 'Cancel Order' : 'Confirm'}
        variant={pendingAction?.type === 'cancel' ? 'destructive' : 'default'}
        loading={isMutating}
        onConfirm={handleConfirm}
      />
    </div>
  )
}
