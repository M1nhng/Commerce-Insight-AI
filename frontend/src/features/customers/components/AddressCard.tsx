/**
 * features/customers/components/AddressCard.tsx
 * Displays a single customer address with actions.
 */
import { MapPin, Phone, Star, Pencil, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { CustomerAddressResponse } from '@/types/customer.types'

interface AddressCardProps {
  address: CustomerAddressResponse
  canWrite?: boolean
  onEdit?: (address: CustomerAddressResponse) => void
  onDelete?: (address: CustomerAddressResponse) => void
  onSetDefault?: (address: CustomerAddressResponse) => void
}

export function AddressCard({ address, canWrite, onEdit, onDelete, onSetDefault }: AddressCardProps) {
  const isShipping = address.type === 'SHIPPING'

  const fullAddress = [
    address.addressLine,
    address.ward,
    address.district,
    address.province,
    address.country,
  ].filter(Boolean).join(', ')

  return (
    <div
      className="relative p-4 rounded-xl border transition-colors"
      style={{
        background: 'var(--bg-surface)',
        borderColor: address.isDefault ? 'var(--accent-500)' : 'var(--border-default)',
        boxShadow: address.isDefault ? '0 0 0 1px var(--accent-500)' : undefined,
      }}
    >
      {/* Type label + default badge */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <span
            className="text-xs font-semibold uppercase tracking-wide px-2 py-0.5 rounded"
            style={{
              background: isShipping ? 'var(--info-bg)' : 'var(--bg-overlay)',
              color: isShipping ? 'var(--info)' : 'var(--text-secondary)',
            }}
          >
            {isShipping ? '🚚 Shipping' : '💳 Billing'}
          </span>
          {address.isDefault && (
            <span
              className="flex items-center gap-1 text-xs font-medium px-2 py-0.5 rounded"
              style={{ background: 'var(--accent-500)20', color: 'var(--accent-400)' }}
            >
              <Star className="h-3 w-3 fill-current" /> Default
            </span>
          )}
        </div>

        {/* Actions */}
        {canWrite && (
          <div className="flex items-center gap-1">
            {!address.isDefault && onSetDefault && (
              <Button
                variant="ghost"
                size="sm"
                className="h-7 text-xs gap-1"
                style={{ color: 'var(--accent-400)' }}
                onClick={() => onSetDefault(address)}
              >
                <Star className="h-3 w-3" /> Set default
              </Button>
            )}
            {onEdit && (
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                style={{ color: 'var(--text-muted)' }}
                onClick={() => onEdit(address)}
              >
                <Pencil className="h-3.5 w-3.5" />
              </Button>
            )}
            {onDelete && (
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                style={{ color: 'var(--error)' }}
                onClick={() => onDelete(address)}
              >
                <Trash2 className="h-3.5 w-3.5" />
              </Button>
            )}
          </div>
        )}
      </div>

      {/* Recipient */}
      <p className="font-semibold text-body-sm mb-1" style={{ color: 'var(--text-primary)' }}>
        {address.recipientName}
      </p>

      {/* Phone */}
      {address.phone && (
        <p className="flex items-center gap-1.5 text-body-sm mb-1" style={{ color: 'var(--text-secondary)' }}>
          <Phone className="h-3.5 w-3.5 shrink-0" />
          {address.phone}
        </p>
      )}

      {/* Address */}
      <p className="flex items-start gap-1.5 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
        <MapPin className="h-3.5 w-3.5 shrink-0 mt-0.5" />
        {fullAddress}
      </p>
    </div>
  )
}
