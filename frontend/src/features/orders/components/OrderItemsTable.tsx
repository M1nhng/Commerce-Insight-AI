/**
 * features/orders/components/OrderItemsTable.tsx
 * Line items table within OrderDetailPage.
 * Shows unit price, quantity, discount, subtotal with totals footer.
 */
import type { OrderResponse } from '@/types/order.types'

interface OrderItemsTableProps {
  order: OrderResponse
}

function fmt(amount: number, currency: string) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: currency === 'VND' ? 'VND' : currency,
    maximumFractionDigits: currency === 'VND' ? 0 : 2,
  }).format(amount)
}

export function OrderItemsTable({ order }: OrderItemsTableProps) {
  const { items, subtotal, discount, shippingFee, tax, total, currency } = order

  return (
    <div
      className="rounded-xl border overflow-hidden"
      style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
    >
      <div className="overflow-x-auto">
        <table className="w-full text-body-sm">
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)' }}>
              {['Product', 'SKU', 'Unit Price', 'Qty', 'Discount', 'Subtotal'].map((h) => (
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
            {items.map((item, idx) => (
              <tr
                key={item.id}
                style={{
                  borderBottom: '1px solid var(--border-subtle)',
                  background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
                }}
              >
                <td className="px-4 py-3 font-medium" style={{ color: 'var(--text-primary)' }}>
                  {item.productNameSnapshot}
                </td>
                <td className="px-4 py-3 font-mono text-caption" style={{ color: 'var(--accent-400)' }}>
                  {item.skuSnapshot}
                </td>
                <td className="px-4 py-3" style={{ color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                  {fmt(item.unitPrice, currency)}
                </td>
                <td className="px-4 py-3 text-center" style={{ color: 'var(--text-primary)' }}>
                  {item.quantity}
                </td>
                <td className="px-4 py-3" style={{ color: item.discountAmount > 0 ? 'var(--warning)' : 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                  {item.discountAmount > 0 ? `- ${fmt(item.discountAmount, currency)}` : '—'}
                </td>
                <td className="px-4 py-3 font-semibold" style={{ color: 'var(--text-primary)', whiteSpace: 'nowrap' }}>
                  {fmt(item.subtotal, currency)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Totals Footer */}
      <div
        className="border-t px-4 py-4 space-y-2"
        style={{ borderColor: 'var(--border-subtle)', background: 'var(--bg-elevated)' }}
      >
        {[
          { label: 'Subtotal',     value: subtotal,    show: true },
          { label: 'Discount',     value: -discount,   show: discount > 0 },
          { label: 'Shipping Fee', value: shippingFee, show: true },
          { label: 'Tax',          value: tax,         show: tax > 0 },
        ].filter((r) => r.show).map((row) => (
          <div key={row.label} className="flex justify-between text-body-sm">
            <span style={{ color: 'var(--text-secondary)' }}>{row.label}</span>
            <span style={{ color: row.value < 0 ? 'var(--warning)' : 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
              {row.value < 0 ? `- ${fmt(Math.abs(row.value), currency)}` : fmt(row.value, currency)}
            </span>
          </div>
        ))}
        <div
          className="flex justify-between pt-2 border-t font-semibold text-body-md"
          style={{ borderColor: 'var(--border-default)' }}
        >
          <span style={{ color: 'var(--text-primary)' }}>Total</span>
          <span style={{ color: 'var(--accent-400)', fontSize: '1.1rem', whiteSpace: 'nowrap' }}>
            {fmt(total, currency)}
          </span>
        </div>
      </div>
    </div>
  )
}
