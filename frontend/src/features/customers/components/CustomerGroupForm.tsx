/**
 * features/customers/components/CustomerGroupForm.tsx
 * Create/edit form for customer groups — used inside a Dialog
 */
import { useEffect } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription,
} from '@/components/ui/dialog'
import type { CustomerGroupResponse } from '@/types/customer.types'

// ── Schema ────────────────────────────────────────────────────────────────

const schema = z.object({
  code:        z.string().min(1, 'Code is required').max(50, 'Max 50 chars'),
  name:        z.string().min(1, 'Name is required').max(150, 'Max 150 chars'),
  description: z.string().max(500).optional().or(z.literal('')),
  status:      z.enum(['ACTIVE', 'INACTIVE']),
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

interface CustomerGroupFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  group?: CustomerGroupResponse | null
  isSubmitting?: boolean
  onSubmit: (values: FormValues) => void
}

export function CustomerGroupForm({
  open, onOpenChange, group, isSubmitting, onSubmit,
}: CustomerGroupFormProps) {
  const isEdit = !!group

  const { register, control, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { code: '', name: '', description: '', status: 'ACTIVE' },
  })

  useEffect(() => {
    if (group) {
      reset({
        code:        group.code,
        name:        group.name,
        description: group.description ?? '',
        status:      group.status,
      })
    } else {
      reset({ code: '', name: '', description: '', status: 'ACTIVE' })
    }
  }, [group, open, reset])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="max-w-md"
        style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}
      >
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)' }}>
            {isEdit ? 'Edit Customer Group' : 'Create Customer Group'}
          </DialogTitle>
          <DialogDescription style={{ color: 'var(--text-secondary)' }}>
            {isEdit ? 'Update the group details.' : 'Add a new customer group to your catalog.'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-2">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Code" required error={errors.code?.message}>
              <Input
                {...register('code')}
                style={inputStyle}
                placeholder="VIP"
                disabled={isEdit}
                className={isEdit ? 'opacity-60 cursor-not-allowed' : ''}
              />
            </Field>
            <Field label="Status" error={errors.status?.message}>
              <Controller
                name="status"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger style={inputStyle}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                      <SelectItem value="ACTIVE"   style={{ color: 'var(--success)' }}>Active</SelectItem>
                      <SelectItem value="INACTIVE" style={{ color: 'var(--text-muted)' }}>Inactive</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
            </Field>
          </div>

          <Field label="Name" required error={errors.name?.message}>
            <Input {...register('name')} style={inputStyle} placeholder="VIP Customers" />
          </Field>

          <Field label="Description" error={errors.description?.message}>
            <Textarea
              {...register('description')}
              style={{ ...inputStyle, resize: 'none' }}
              placeholder="Optional description..."
              rows={3}
            />
          </Field>

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
              {isEdit ? 'Save Changes' : 'Create Group'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
