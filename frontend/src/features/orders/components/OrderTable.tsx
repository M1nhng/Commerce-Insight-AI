/**
 * features/orders/components/OrderTable.tsx
 * Data table for the order list page.
 * Pattern mirrors CustomerTable.tsx exactly.
 */
import { useNavigate } from 'react-router-dom'
import { MoreHorizontal, Eye, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { TableSkeleton } from '@/components/common/TableSkeleton'
import { OrderStatusBadge } from './OrderStatusBadge'
import { PaymentStatusBadge } from './PaymentStatusBadge'
import type { OrderSummaryResponse } from '@/types/order.types'

interface OrderTableProps {
  orders: OrderSummaryResponse[]
  isLoading: boolean
  canWrite: boolean
  onCancel: (order: OrderSummaryResponse) => void
}

const CANCELLABLE = new Set(['PENDING', 'CONFIRMED', 'PROCESSING'])

function formatCurrency(amount: number, currency: string) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: currency === 'VND' ? 'VND' : currency,
    maximumFractionDigits: currency === 'VND' ? 0 : 2,
  }).format(amount)
}

export function OrderTable({ orders, isLoading, canWrite, onCancel }: OrderTableProps) {
  const navigate = useNavigate()

  if (isLoading) return <TableSkeleton rows={8} cols={7} />

  if (orders.length === 0) {
    return (
      <div
        className="flex flex-col items-center justify-center py-20 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <div className="text-5xl mb-4">🛒</div>
        <h3 className="text-heading-3 mb-1" style={{ color: 'var(--text-primary)' }}>
          No orders found
        </h3>
        <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
          Try adjusting your filters or create your first order.
        </p>
      </div>
    )
  }

  return (
    <div
      className="rounded-xl border overflow-hidden"
      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
    >
      <div className="overflow-x-auto">
        <table className="w-full text-body-sm">
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)' }}>
              {['Order #', 'Customer', 'Items', 'Total', 'Payment', 'Status', 'Created', ''].map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left font-medium text-caption uppercase tracking-wide"
                  style={{ color: 'var(--text-muted)', whiteSpace: 'nowrap' }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {orders.map((order, idx) => (
              <tr
                key={order.id}
                className="transition-colors cursor-pointer hover:bg-[var(--bg-overlay)]"
                style={{
                  borderBottom: '1px solid var(--border-subtle)',
                  background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
                }}
                onClick={() => navigate(`/orders/${order.id}`)}
              >
                {/* Order Number */}
                <td className="px-4 py-3 font-mono text-caption font-semibold" style={{ color: 'var(--accent-400)' }}>
                  {order.orderNumber}
                </td>

                {/* Customer */}
                <td className="px-4 py-3 font-medium" style={{ color: 'var(--text-primary)' }}>
                  {order.customerName}
                </td>

                {/* Item Count */}
                <td className="px-4 py-3 text-center" style={{ color: 'var(--text-secondary)' }}>
                  {order.itemCount}
                </td>

                {/* Total */}
                <td className="px-4 py-3 font-semibold" style={{ color: 'var(--text-primary)', whiteSpace: 'nowrap' }}>
                  {formatCurrency(order.total, order.currency)}
                </td>

                {/* Payment Status */}
                <td className="px-4 py-3">
                  <PaymentStatusBadge status={order.paymentStatus} />
                </td>

                {/* Order Status */}
                <td className="px-4 py-3">
                  <OrderStatusBadge status={order.status} />
                </td>

                {/* Created */}
                <td className="px-4 py-3 text-caption" style={{ color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                  {new Date(order.createdAt).toLocaleDateString('en-GB', {
                    day: '2-digit', month: 'short', year: 'numeric',
                  })}
                </td>

                {/* Actions */}
                <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost" size="icon" className="h-7 w-7"
                        style={{ color: 'var(--text-muted)' }}
                      >
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                      align="end"
                      style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
                    >
                      <DropdownMenuItem
                        className="gap-2 cursor-pointer"
                        style={{ color: 'var(--text-primary)' }}
                        onClick={() => navigate(`/orders/${order.id}`)}
                      >
                        <Eye className="h-4 w-4" /> View Detail
                      </DropdownMenuItem>

                      {canWrite && CANCELLABLE.has(order.status) && (
                        <>
                          <DropdownMenuSeparator style={{ background: 'var(--border-subtle)' }} />
                          <DropdownMenuItem
                            className="gap-2 cursor-pointer"
                            style={{ color: 'var(--error)' }}
                            onClick={() => onCancel(order)}
                          >
                            <XCircle className="h-4 w-4" /> Cancel Order
                          </DropdownMenuItem>
                        </>
                      )}
                    </DropdownMenuContent>
                  </DropdownMenu>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
