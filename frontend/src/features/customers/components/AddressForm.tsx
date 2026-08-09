/**
 * features/customers/components/AddressForm.tsx
 * Form for adding / editing a customer address — React Hook Form + Zod
 */
import { useEffect } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from '@/components/ui/dialog'
import type { CustomerAddressResponse } from '@/types/customer.types'

// ── Schema ────────────────────────────────────────────────────────────────

const schema = z.object({
  type:          z.enum(['SHIPPING', 'BILLING']),
  recipientName: z.string().min(1, 'Recipient name is required').max(200),
  phone:         z.string().max(20).optional().or(z.literal('')),
  addressLine:   z.string().min(1, 'Address is required').max(500),
  ward:          z.string().max(150).optional().or(z.literal('')),
  district:      z.string().max(150).optional().or(z.literal('')),
  province:      z.string().max(150).optional().or(z.literal('')),
  country:       z.string().max(100).optional().or(z.literal('')),
  isDefault:     z.boolean().optional(),
})

type FormValues = z.infer<typeof schema>

const inputStyle = {
  background: 'var(--bg-elevated)',
  borderColor: 'var(--border-default)',
  color: 'var(--text-primary)',
}

function Field({ label, error, required, children }: {
  label: string; error?: string; required?: boolean; children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
        {label}{required && <span style={{ color: 'var(--error)' }}> *</span>}
      </Label>
      {children}
      {error && <p className="text-xs" style={{ color: 'var(--error)' }}>{error}</p>}
    </div>
  )
}

// ── Component ─────────────────────────────────────────────────────────────

interface AddressFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  address?: CustomerAddressResponse | null
  isSubmitting?: boolean
  onSubmit: (values: FormValues) => void
}

export function AddressForm({ open, onOpenChange, address, isSubmitting, onSubmit }: AddressFormProps) {
  const isEdit = !!address

  const { register, control, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      type: 'SHIPPING',
      recipientName: '',
      phone: '',
      addressLine: '',
      ward: '', district: '', province: '', country: 'VN',
      isDefault: false,
    },
  })

  useEffect(() => {
    if (address) {
      reset({
        type: address.type,
        recipientName: address.recipientName,
        phone: address.phone ?? '',
        addressLine: address.addressLine,
        ward: address.ward ?? '',
        district: address.district ?? '',
        province: address.province ?? '',
        country: address.country ?? 'VN',
        isDefault: address.isDefault,
      })
    } else {
      reset({
        type: 'SHIPPING', recipientName: '', phone: '',
        addressLine: '', ward: '', district: '', province: '', country: 'VN', isDefault: false,
      })
    }
  }, [address, open, reset])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="max-w-xl"
        style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
      >
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)' }}>
            {isEdit ? 'Edit Address' : 'Add Address'}
          </DialogTitle>
          <DialogDescription style={{ color: 'var(--text-secondary)' }}>
            {isEdit ? 'Update the address details below.' : 'Fill in the address details.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-2">
          {/* Type (only for new address) */}
          {!isEdit && (
            <Field label="Address Type" required error={errors.type?.message}>
              <Controller
                name="type"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger style={inputStyle}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                      <SelectItem value="SHIPPING" style={{ color: 'var(--text-primary)' }}>🚚 Shipping</SelectItem>
                      <SelectItem value="BILLING"  style={{ color: 'var(--text-primary)' }}>💳 Billing</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
          )}

          {/* Recipient + Phone */}
          <div className="grid grid-cols-2 gap-3">
            <Field label="Recipient Name" required error={errors.recipientName?.message}>
              <Input {...register('recipientName')} style={inputStyle} placeholder="Full name" />
            </Field>
            <Field label="Phone" error={errors.phone?.message}>
              <Input {...register('phone')} style={inputStyle} placeholder="+84..." />
            </Field>
          </div>

          {/* Address line */}
          <Field label="Street Address" required error={errors.addressLine?.message}>
            <Input {...register('addressLine')} style={inputStyle} placeholder="123 Street Name" />
          </Field>

          {/* Ward + District */}
          <div className="grid grid-cols-2 gap-3">
            <Field label="Ward" error={errors.ward?.message}>
              <Input {...register('ward')} style={inputStyle} placeholder="Ward / Area" />
            </Field>
            <Field label="District" error={errors.district?.message}>
              <Input {...register('district')} style={inputStyle} placeholder="District" />
            </Field>
          </div>

          {/* Province + Country */}
          <div className="grid grid-cols-2 gap-3">
            <Field label="Province / City" error={errors.province?.message}>
              <Input {...register('province')} style={inputStyle} placeholder="Ho Chi Minh City" />
            </Field>
            <Field label="Country" error={errors.country?.message}>
              <Input {...register('country')} style={inputStyle} placeholder="VN" />
            </Field>
          </div>

          {/* Default toggle */}
          <div className="flex items-center gap-2 pt-1">
            <input
              id="isDefault"
              type="checkbox"
              {...register('isDefault')}
              className="h-4 w-4 rounded"
              style={{ accentColor: 'var(--accent-500)' }}
            />
            <Label htmlFor="isDefault" style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', cursor: 'pointer' }}>
              Set as default address for this type
            </Label>
          </div>

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isSubmitting}
              style={{ background: 'var(--bg-overlay)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting}
              style={{ background: 'var(--accent-500)', color: '#fff' }}
            >
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {isEdit ? 'Save Changes' : 'Add Address'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export type { FormValues as AddressFormValues }
