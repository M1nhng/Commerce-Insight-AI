/**
 * features/orders/components/OrderAddressCard.tsx
 * Displays a shipping or billing address snapshot.
 */
import { MapPin } from 'lucide-react'
import type { OrderAddressResponse } from '@/types/order.types'

interface OrderAddressCardProps {
  address: OrderAddressResponse | null | undefined
  title: string
}

export function OrderAddressCard({ address, title }: OrderAddressCardProps) {
  return (
    <div
      className="rounded-xl border p-4 space-y-2"
      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
    >
      <div className="flex items-center gap-2">
        <MapPin className="h-4 w-4 shrink-0" style={{ color: 'var(--accent-400)' }} />
        <h4 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
          {title}
        </h4>
      </div>

      {address ? (
        <div className="text-body-sm space-y-0.5" style={{ color: 'var(--text-secondary)' }}>
          <p className="font-medium" style={{ color: 'var(--text-primary)' }}>
            {address.recipientName}
          </p>
          {address.phone && <p>{address.phone}</p>}
          <p>{address.addressLine}</p>
          {(address.ward || address.district || address.province) && (
            <p>
              {[address.ward, address.district, address.province]
                .filter(Boolean)
                .join(', ')}
            </p>
          )}
          <p>{address.country}</p>
        </div>
      ) : (
        <p className="text-body-sm" style={{ color: 'var(--text-muted)' }}>
          No address provided
        </p>
      )}
    </div>
  )
}
