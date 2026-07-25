/**
 * features/inventory/components/TransferDialog.tsx
 * Atomic stock transfer between warehouses.
 */
import { useEffect } from 'react'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowRight } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useTransferStock } from '../hooks/useInventory'
import { useWarehouseOptions } from '../hooks/useWarehouses'
import type { InventoryResponse } from '@/types/inventory.types'

// ── Schema ────────────────────────────────────────────────────────────────

const schema = z
  .object({
    sourceWarehouseId:      z.string().min(1, 'Select source warehouse'),
    destinationWarehouseId: z.string().min(1, 'Select destination warehouse'),
    quantity: z
      .number({ invalid_type_error: 'Enter a positive number' })
      .int()
      .positive('Must be greater than 0'),
    notes: z.string().max(500).optional(),
  })
  .refine(
    (v) => v.sourceWarehouseId !== v.destinationWarehouseId,
    { message: 'Source and destination must be different', path: ['destinationWarehouseId'] }
  )

type FormValues = z.infer<typeof schema>

interface TransferDialogProps {
  /** Pre-selected inventory row (determines product + default source warehouse) */
  inventory: InventoryResponse | null
  open: boolean
  onClose: () => void
}

export function TransferDialog({ inventory, open, onClose }: TransferDialogProps) {
  const transferMutation = useTransferStock()
  const { data: warehouses = [] } = useWarehouseOptions()

  const { register, handleSubmit, watch, control, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      sourceWarehouseId: inventory?.warehouseId ?? '',
      destinationWarehouseId: '',
      quantity: 1,
      notes: '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        sourceWarehouseId: inventory?.warehouseId ?? '',
        destinationWarehouseId: '',
        quantity: 1,
        notes: '',
      })
    }
  }, [open, inventory, reset])

  const quantity  = watch('quantity') ?? 0
  const available = inventory?.availableQuantity ?? 0

  const onSubmit = async (values: FormValues) => {
    if (!inventory) return
    await transferMutation.mutateAsync({
      productId: inventory.productId,
      sourceWarehouseId: values.sourceWarehouseId,
      destinationWarehouseId: values.destinationWarehouseId,
      quantity: values.quantity,
      notes: values.notes || undefined,
    })
    onClose()
  }

  const isLoading = transferMutation.isPending

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-default)',
          borderRadius: 16,
          maxWidth: 480,
          width: '100%',
        }}
      >
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)', fontSize: '1.0625rem' }}>
            Transfer Stock
          </DialogTitle>
          {inventory && (
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: 4 }}>
              {inventory.productName} · SKU: {inventory.productSku}
            </p>
          )}
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 8 }}>
          {/* Warehouse selector row */}
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
            {/* Source */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                From Warehouse <span style={{ color: 'var(--error)' }}>*</span>
              </Label>
              <Controller
                name="sourceWarehouseId"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger
                      style={{
                        background: 'var(--bg-elevated)',
                        border: `1px solid ${errors.sourceWarehouseId ? 'var(--error)' : 'var(--border-default)'}`,
                        color: 'var(--text-primary)',
                        borderRadius: 8,
                        height: 38,
                        fontSize: '0.875rem',
                      }}
                    >
                      <SelectValue placeholder="Select…" />
                    </SelectTrigger>
                    <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                      {warehouses.map((w) => (
                        <SelectItem key={w.id} value={w.id} style={{ color: 'var(--text-primary)' }}>
                          {w.name} ({w.code})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.sourceWarehouseId && (
                <p style={{ fontSize: '0.75rem', color: 'var(--error)' }}>{errors.sourceWarehouseId.message}</p>
              )}
            </div>

            {/* Arrow */}
            <div style={{ paddingTop: 28, color: 'var(--text-muted)', flexShrink: 0 }}>
              <ArrowRight size={18} />
            </div>

            {/* Destination */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                To Warehouse <span style={{ color: 'var(--error)' }}>*</span>
              </Label>
              <Controller
                name="destinationWarehouseId"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger
                      style={{
                        background: 'var(--bg-elevated)',
                        border: `1px solid ${errors.destinationWarehouseId ? 'var(--error)' : 'var(--border-default)'}`,
                        color: 'var(--text-primary)',
                        borderRadius: 8,
                        height: 38,
                        fontSize: '0.875rem',
                      }}
                    >
                      <SelectValue placeholder="Select…" />
                    </SelectTrigger>
                    <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                      {warehouses.map((w) => (
                        <SelectItem key={w.id} value={w.id} style={{ color: 'var(--text-primary)' }}>
                          {w.name} ({w.code})
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.destinationWarehouseId && (
                <p style={{ fontSize: '0.75rem', color: 'var(--error)' }}>{errors.destinationWarehouseId.message}</p>
              )}
            </div>
          </div>

          {/* Available stock hint */}
          {inventory && (
            <div
              style={{
                background: 'var(--bg-elevated)',
                borderRadius: 8,
                padding: '10px 14px',
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '0.8125rem',
              }}
            >
              <span style={{ color: 'var(--text-secondary)' }}>Available in source:</span>
              <span
                style={{
                  fontWeight: 700,
                  fontFamily: 'JetBrains Mono, monospace',
                  color: available === 0 ? 'var(--error)' : 'var(--success)',
                }}
              >
                {available.toLocaleString()} units
              </span>
            </div>
          )}

          {/* Quantity */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
              Quantity to Transfer <span style={{ color: 'var(--error)' }}>*</span>
            </Label>
            <Input
              type="number"
              min={1}
              max={available}
              {...register('quantity', { valueAsNumber: true })}
              style={{
                background: 'var(--bg-elevated)',
                border: `1px solid ${
                  errors.quantity
                    ? 'var(--error)'
                    : quantity > available
                    ? 'var(--warning)'
                    : 'var(--border-default)'
                }`,
                color: 'var(--text-primary)',
                borderRadius: 8,
              }}
            />
            {errors.quantity && (
              <p style={{ fontSize: '0.75rem', color: 'var(--error)' }}>{errors.quantity.message}</p>
            )}
            {quantity > available && !errors.quantity && (
              <p style={{ fontSize: '0.75rem', color: 'var(--warning)' }}>
                Exceeds available stock ({available.toLocaleString()} units)
              </p>
            )}
          </div>

          {/* Notes */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
              Notes (optional)
            </Label>
            <Textarea
              {...register('notes')}
              placeholder="Reason for transfer…"
              rows={2}
              style={{
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border-default)',
                color: 'var(--text-primary)',
                borderRadius: 8,
                fontSize: '0.875rem',
                resize: 'vertical',
              }}
            />
          </div>

          <DialogFooter style={{ marginTop: 8 }}>
            <Button
              type="button"
              variant="ghost"
              onClick={onClose}
              style={{ color: 'var(--text-secondary)' }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isLoading || quantity > available || available === 0}
              style={{
                background: 'var(--accent-500)',
                color: '#fff',
                borderRadius: 8,
                opacity: isLoading ? 0.7 : 1,
              }}
            >
              {isLoading ? 'Transferring…' : 'Transfer Stock'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
