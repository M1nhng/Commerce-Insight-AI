/**
 * features/inventory/components/AdjustmentDialog.tsx
 *
 * Two-mode dialog:
 *   • DIRECT  — immediately adjusts stock (ADMIN/MANAGER)
 *   • REQUEST — submits a PENDING request for approval (all roles)
 */
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useAdjustStock } from '../hooks/useInventory'
import { useRequestAdjustment } from '../hooks/useStockAdjustments'
import type { InventoryResponse } from '@/types/inventory.types'

// ── Schema ────────────────────────────────────────────────────────────────

const schema = z.object({
  quantity: z
    .number({ invalid_type_error: 'Enter a non-zero number' })
    .int('Must be a whole number')
    .refine((v) => v !== 0, 'Delta cannot be zero'),
  notes: z.string().max(500, 'Max 500 chars').optional(),
  lowStockThreshold: z.number().int().positive().optional().nullable(),
})

type FormValues = z.infer<typeof schema>

interface AdjustmentDialogProps {
  inventory: InventoryResponse | null
  mode: 'direct' | 'request'
  open: boolean
  onClose: () => void
}

export function AdjustmentDialog({ inventory, mode, open, onClose }: AdjustmentDialogProps) {
  const adjustMutation  = useAdjustStock()
  const requestMutation = useRequestAdjustment()

  const { register, handleSubmit, watch, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { quantity: 0, notes: '', lowStockThreshold: null },
  })

  // Reset form on open
  useEffect(() => {
    if (open) reset({ quantity: 0, notes: '', lowStockThreshold: null })
  }, [open, reset])

  const delta    = watch('quantity') ?? 0
  const current  = inventory?.quantity ?? 0
  const projected = current + delta

  const deltaPositive = delta > 0
  const deltaNegative = delta < 0

  const onSubmit = async (values: FormValues) => {
    if (!inventory) return
    if (mode === 'direct') {
      await adjustMutation.mutateAsync({
        id: inventory.id,
        data: {
          quantity: values.quantity,
          notes: values.notes || undefined,
          lowStockThreshold: values.lowStockThreshold ?? undefined,
        },
      })
    } else {
      await requestMutation.mutateAsync({
        inventoryId: inventory.id,
        quantityDelta: values.quantity,
        reason: values.notes ?? 'Manual adjustment request',
      })
    }
    onClose()
  }

  const isLoading = adjustMutation.isPending || requestMutation.isPending

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-default)',
          borderRadius: 16,
          maxWidth: 440,
          width: '100%',
        }}
      >
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)', fontSize: '1.0625rem' }}>
            {mode === 'direct' ? 'Adjust Stock' : 'Request Stock Adjustment'}
          </DialogTitle>
          {inventory && (
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: 4 }}>
              {inventory.productName} · {inventory.warehouseName}
            </p>
          )}
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 8 }}>
          {/* Current stock display */}
          <div
            style={{
              background: 'var(--bg-elevated)',
              borderRadius: 10,
              padding: '12px 16px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div>
              <p style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginBottom: 2 }}>Current Stock</p>
              <p style={{ fontSize: '1.5rem', fontWeight: 700, fontFamily: 'JetBrains Mono, monospace', color: 'var(--text-primary)' }}>
                {current.toLocaleString()}
              </p>
            </div>
            <div style={{ textAlign: 'right' }}>
              <p style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', marginBottom: 2 }}>After Adjustment</p>
              <p
                style={{
                  fontSize: '1.5rem',
                  fontWeight: 700,
                  fontFamily: 'JetBrains Mono, monospace',
                  color: projected < 0
                    ? 'var(--error)'
                    : projected === 0
                    ? 'var(--warning)'
                    : 'var(--success)',
                  transition: 'color 0.2s',
                }}
              >
                {projected.toLocaleString()}
              </p>
            </div>
          </div>

          {/* Delta input */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
              Quantity Delta <span style={{ color: 'var(--error)' }}>*</span>
            </Label>
            <div style={{ position: 'relative' }}>
              <div
                style={{
                  position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)',
                  color: deltaPositive ? 'var(--success)' : deltaNegative ? 'var(--error)' : 'var(--text-muted)',
                  pointerEvents: 'none',
                }}
              >
                {deltaPositive ? <TrendingUp size={16} /> : deltaNegative ? <TrendingDown size={16} /> : <Minus size={16} />}
              </div>
              <Input
                type="number"
                {...register('quantity', { valueAsNumber: true })}
                placeholder="e.g. +50 or -10"
                style={{
                  paddingLeft: 36,
                  background: 'var(--bg-elevated)',
                  border: `1px solid ${errors.quantity ? 'var(--error)' : 'var(--border-default)'}`,
                  color: 'var(--text-primary)',
                  borderRadius: 8,
                }}
              />
            </div>
            {errors.quantity && (
              <p style={{ fontSize: '0.75rem', color: 'var(--error)' }}>{errors.quantity.message}</p>
            )}
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Use positive values to add stock, negative to remove.
            </p>
          </div>

          {/* Notes / reason */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
              {mode === 'request' ? 'Reason *' : 'Notes (optional)'}
            </Label>
            <Textarea
              {...register('notes')}
              placeholder={mode === 'request' ? 'Why is this adjustment needed?' : 'Reason for adjustment...'}
              rows={3}
              style={{
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border-default)',
                color: 'var(--text-primary)',
                borderRadius: 8,
                resize: 'vertical',
                fontSize: '0.875rem',
              }}
            />
          </div>

          {/* Low stock threshold (direct mode only) */}
          {mode === 'direct' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                Low Stock Threshold (optional)
              </Label>
              <Input
                type="number"
                {...register('lowStockThreshold', { valueAsNumber: true })}
                placeholder="e.g. 10"
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border-default)',
                  color: 'var(--text-primary)',
                  borderRadius: 8,
                }}
              />
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Alert when stock falls below this number.
              </p>
            </div>
          )}

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
              disabled={isLoading || projected < 0}
              style={{
                background: mode === 'direct' ? 'var(--accent-500)' : 'var(--warning)',
                color: '#fff',
                borderRadius: 8,
                opacity: isLoading ? 0.7 : 1,
              }}
            >
              {isLoading
                ? 'Processing…'
                : mode === 'direct'
                ? 'Apply Adjustment'
                : 'Submit Request'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
