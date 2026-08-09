/**
 * features/customers/components/CustomerForm.tsx
 * Shared create/edit form — React Hook Form + Zod
 * Used as both a full page form and in sheets/dialogs
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
import { useCustomerGroupOptions } from '../hooks/useCustomerGroups'
import type { CustomerResponse } from '@/types/customer.types'

// ── Zod Schema ────────────────────────────────────────────────────────────

const schema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100),
  lastName:  z.string().min(1, 'Last name is required').max(100),
  email:     z.union([z.string().email('Invalid email address'), z.literal('')]).optional(),
  phone:     z.string().max(20, 'Phone too long').optional().or(z.literal('')),
  dateOfBirth: z.string().optional().or(z.literal('')),
  gender:    z.enum(['MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY', '__none__']).optional(),
  groupId:   z.string().optional().or(z.literal('__none__')),
  customerCode: z.string().max(50).optional().or(z.literal('')),
})

type FormValues = z.infer<typeof schema>

// ── Field helpers ─────────────────────────────────────────────────────────

const inputStyle = {
  background: 'var(--bg-elevated)',
  borderColor: 'var(--border-default)',
  color: 'var(--text-primary)',
}

const labelStyle = { color: 'var(--text-secondary)', fontSize: '0.8125rem' }

function Field({ label, error, required, children }: {
  label: string
  error?: string
  required?: boolean
  children: React.ReactNode
}) {
  return (
    <div className="space-y-1.5">
      <Label style={labelStyle}>
        {label}{required && <span style={{ color: 'var(--error)' }}> *</span>}
      </Label>
      {children}
      {error && <p className="text-caption" style={{ color: 'var(--error)' }}>{error}</p>}
    </div>
  )
}

// ── Component ─────────────────────────────────────────────────────────────

interface CustomerFormProps {
  customer?: CustomerResponse | null
  isSubmitting?: boolean
  onSubmit: (values: FormValues) => void
  onCancel: () => void
}

export function CustomerForm({ customer, isSubmitting, onSubmit, onCancel }: CustomerFormProps) {
  const { data: groupOptions = [] } = useCustomerGroupOptions()

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      dateOfBirth: '',
      gender: '__none__',
      groupId: '__none__',
      customerCode: '',
    },
  })

  // Pre-fill when editing
  useEffect(() => {
    if (customer) {
      reset({
        firstName:    customer.firstName,
        lastName:     customer.lastName,
        email:        customer.email ?? '',
        phone:        customer.phone ?? '',
        dateOfBirth:  customer.dateOfBirth ?? '',
        gender:       customer.gender ?? '__none__',
        groupId:      customer.groupId ?? '__none__',
        customerCode: customer.customerCode ?? '',
      })
    }
  }, [customer, reset])

  const handleFormSubmit = (values: FormValues) => {
    onSubmit({
      ...values,
      email:       values.email || undefined,
      phone:       values.phone || undefined,
      dateOfBirth: values.dateOfBirth || undefined,
      gender:      values.gender === '__none__' ? undefined : values.gender,
      groupId:     values.groupId === '__none__' ? undefined : values.groupId,
      customerCode: values.customerCode || undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-5">
      {/* Name row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Field label="First Name" error={errors.firstName?.message} required>
          <Input {...register('firstName')} style={inputStyle} placeholder="Jane" />
        </Field>
        <Field label="Last Name" error={errors.lastName?.message} required>
          <Input {...register('lastName')} style={inputStyle} placeholder="Smith" />
        </Field>
      </div>

      {/* Email + Phone */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Field label="Email" error={errors.email?.message}>
          <Input {...register('email')} type="email" style={inputStyle} placeholder="jane@example.com" />
        </Field>
        <Field label="Phone" error={errors.phone?.message}>
          <Input {...register('phone')} style={inputStyle} placeholder="+84 901 234 567" />
        </Field>
      </div>

      {/* DOB + Gender */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Field label="Date of Birth" error={errors.dateOfBirth?.message}>
          <Input {...register('dateOfBirth')} type="date" style={inputStyle} />
        </Field>
        <Field label="Gender" error={errors.gender?.message}>
          <Controller
            name="gender"
            control={control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger style={inputStyle}>
                  <SelectValue placeholder="Select gender" />
                </SelectTrigger>
                <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                  <SelectItem value="__none__" style={{ color: 'var(--text-muted)' }}>Not specified</SelectItem>
                  <SelectItem value="MALE"   style={{ color: 'var(--text-primary)' }}>Male</SelectItem>
                  <SelectItem value="FEMALE" style={{ color: 'var(--text-primary)' }}>Female</SelectItem>
                  <SelectItem value="OTHER"  style={{ color: 'var(--text-primary)' }}>Other</SelectItem>
                  <SelectItem value="PREFER_NOT_TO_SAY" style={{ color: 'var(--text-primary)' }}>Prefer not to say</SelectItem>
                </SelectContent>
              </Select>
            )}
          />
        </Field>
      </div>

      {/* Group + Code */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Field label="Customer Group" error={errors.groupId?.message}>
          <Controller
            name="groupId"
            control={control}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger style={inputStyle}>
                  <SelectValue placeholder="Assign to group..." />
                </SelectTrigger>
                <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                  <SelectItem value="__none__" style={{ color: 'var(--text-muted)' }}>No group</SelectItem>
                  {groupOptions.map((g) => (
                    <SelectItem key={g.value} value={g.value} style={{ color: 'var(--text-primary)' }}>
                      {g.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </Field>
        <Field label="Customer Code" error={errors.customerCode?.message}>
          <Input
            {...register('customerCode')}
            style={inputStyle}
            placeholder="Auto-generated if empty"
            disabled={!!customer}
          />
        </Field>
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3 pt-2">
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
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
          {customer ? 'Save Changes' : 'Create Customer'}
        </Button>
      </div>
    </form>
  )
}

export type { FormValues as CustomerFormValues }
