/**
 * features/inventory/components/WarehouseTable.tsx
 * Warehouse management table with create/edit dialog and delete confirmation.
 */
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Plus, Pencil, Trash2, Warehouse, CheckCircle, XCircle } from 'lucide-react'
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
import {
  useWarehouses,
  useCreateWarehouse,
  useUpdateWarehouse,
  useDeleteWarehouse,
} from '../hooks/useWarehouses'
import type { WarehouseResponse } from '@/types/inventory.types'
import { useAuth } from '@/hooks/useAuth'

// ── Schema ────────────────────────────────────────────────────────────────

const schema = z.object({
  name:    z.string().min(1, 'Name is required').max(100),
  code:    z.string().min(1, 'Code is required').max(20).regex(/^[A-Z0-9_-]+$/, 'Use uppercase letters, numbers, _ and - only').transform((v) => v.toUpperCase()),
  address: z.string().max(255).optional().nullable(),
  city:    z.string().max(100).optional().nullable(),
  country: z.string().max(100).optional().nullable(),
  active:  z.boolean().optional().default(true),
})

type FormValues = z.infer<typeof schema>

// ── Warehouse Form Dialog ─────────────────────────────────────────────────

interface WarehouseFormDialogProps {
  warehouse?: WarehouseResponse | null
  open: boolean
  onClose: () => void
}

function WarehouseFormDialog({ warehouse, open, onClose }: WarehouseFormDialogProps) {
  const isEdit = !!warehouse
  const createMutation = useCreateWarehouse()
  const updateMutation = useUpdateWarehouse()

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name:    warehouse?.name    ?? '',
      code:    warehouse?.code    ?? '',
      address: warehouse?.address ?? '',
      city:    warehouse?.city    ?? '',
      country: warehouse?.country ?? '',
      active:  warehouse?.active  ?? true,
    },
  })

  // Sync defaults when warehouse changes
  useState(() => {
    if (open) {
      reset({
        name:    warehouse?.name    ?? '',
        code:    warehouse?.code    ?? '',
        address: warehouse?.address ?? '',
        city:    warehouse?.city    ?? '',
        country: warehouse?.country ?? '',
        active:  warehouse?.active  ?? true,
      })
    }
  })

  const onSubmit = async (values: FormValues) => {
    if (isEdit && warehouse) {
      await updateMutation.mutateAsync({
        id: warehouse.id,
        data: {
          name:    values.name,
          code:    values.code,
          address: values.address ?? null,
          city:    values.city    ?? null,
          country: values.country ?? null,
          active:  values.active ?? true,
        },
      })
    } else {
      await createMutation.mutateAsync({
        name:    values.name,
        code:    values.code,
        address: values.address ?? null,
        city:    values.city    ?? null,
        country: values.country ?? null,
      })
    }
    onClose()
  }

  const isLoading = createMutation.isPending || updateMutation.isPending

  const fieldStyle = {
    background: 'var(--bg-elevated)',
    border: '1px solid var(--border-default)',
    color: 'var(--text-primary)',
    borderRadius: 8,
    height: 38,
    fontSize: '0.875rem',
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-default)',
          borderRadius: 16,
          maxWidth: 500,
          width: '100%',
        }}
      >
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)', fontSize: '1.0625rem' }}>
            {isEdit ? 'Edit Warehouse' : 'New Warehouse'}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: 14, marginTop: 8 }}>
          {/* Name + Code row */}
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 2, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                Name <span style={{ color: 'var(--error)' }}>*</span>
              </Label>
              <Input {...register('name')} placeholder="Main Warehouse" style={{ ...fieldStyle, border: `1px solid ${errors.name ? 'var(--error)' : 'var(--border-default)'}` }} />
              {errors.name && <p style={{ fontSize: '0.75rem', color: 'var(--error)' }}>{errors.name.message}</p>}
            </div>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                Code <span style={{ color: 'var(--error)' }}>*</span>
              </Label>
              <Input {...register('code')} placeholder="WH-MAIN" style={{ ...fieldStyle, fontFamily: 'JetBrains Mono, monospace', textTransform: 'uppercase', border: `1px solid ${errors.code ? 'var(--error)' : 'var(--border-default)'}` }} />
              {errors.code && <p style={{ fontSize: '0.75rem', color: 'var(--error)' }}>{errors.code.message}</p>}
            </div>
          </div>

          {/* Address */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>Address</Label>
            <Input {...register('address')} placeholder="123 Warehouse St" style={fieldStyle} />
          </div>

          {/* City + Country */}
          <div style={{ display: 'flex', gap: 12 }}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>City</Label>
              <Input {...register('city')} placeholder="Ho Chi Minh City" style={fieldStyle} />
            </div>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
              <Label style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>Country</Label>
              <Input {...register('country')} placeholder="Vietnam" style={fieldStyle} />
            </div>
          </div>

          {/* Active toggle (edit only) */}
          {isEdit && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <input
                type="checkbox"
                id="active"
                {...register('active')}
                style={{ width: 16, height: 16, accentColor: 'var(--accent-500)', cursor: 'pointer' }}
              />
              <Label htmlFor="active" style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', cursor: 'pointer', marginBottom: 0 }}>
                Active
              </Label>
            </div>
          )}

          <DialogFooter style={{ marginTop: 8 }}>
            <Button type="button" variant="ghost" onClick={onClose} style={{ color: 'var(--text-secondary)' }}>
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isLoading}
              style={{ background: 'var(--accent-500)', color: '#fff', borderRadius: 8, opacity: isLoading ? 0.7 : 1 }}
            >
              {isLoading ? 'Saving…' : isEdit ? 'Save Changes' : 'Create Warehouse'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// ── Delete Confirmation ────────────────────────────────────────────────────

interface DeleteConfirmProps {
  warehouse: WarehouseResponse | null
  onClose: () => void
}

function DeleteConfirmDialog({ warehouse, onClose }: DeleteConfirmProps) {
  const deleteMutation = useDeleteWarehouse()
  const onConfirm = async () => {
    if (!warehouse) return
    await deleteMutation.mutateAsync(warehouse.id)
    onClose()
  }
  return (
    <Dialog open={!!warehouse} onOpenChange={(v) => !v && onClose()}>
      <DialogContent style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)', borderRadius: 16, maxWidth: 420 }}>
        <DialogHeader>
          <DialogTitle style={{ color: 'var(--text-primary)' }}>Delete Warehouse</DialogTitle>
        </DialogHeader>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: 8 }}>
          Are you sure you want to delete{' '}
          <strong style={{ color: 'var(--text-primary)' }}>{warehouse?.name}</strong>?
          This will fail if the warehouse still has inventory.
        </p>
        <DialogFooter style={{ marginTop: 16 }}>
          <Button variant="ghost" onClick={onClose} style={{ color: 'var(--text-secondary)' }}>Cancel</Button>
          <Button
            onClick={onConfirm}
            disabled={deleteMutation.isPending}
            style={{ background: 'var(--error)', color: '#fff', borderRadius: 8 }}
          >
            {deleteMutation.isPending ? 'Deleting…' : 'Delete'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Main Table ─────────────────────────────────────────────────────────────

export function WarehouseTable() {
  const { user } = useAuth()
  const isAdmin   = user?.role === 'ADMIN'
  const isManager = user?.role === 'ADMIN' || user?.role === 'MANAGER'

  const [page] = useState(0)
  const [size] = useState(20)
  const [formOpen, setFormOpen]               = useState(false)
  const [editTarget, setEditTarget]           = useState<WarehouseResponse | null>(null)
  const [deleteTarget, setDeleteTarget]       = useState<WarehouseResponse | null>(null)

  const { data, isLoading } = useWarehouses({ page, size, sortBy: 'name', sortDir: 'asc' })
  const warehouses = data?.content ?? []

  return (
    <div style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)', borderRadius: 12, overflow: 'hidden' }}>
      {/* Header */}
      <div style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <Warehouse size={18} style={{ color: 'var(--accent-400)' }} />
          <h2 style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)' }}>
            Warehouses
          </h2>
          {!isLoading && (
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', background: 'var(--bg-elevated)', borderRadius: 20, padding: '1px 8px' }}>
              {data?.totalElements ?? 0}
            </span>
          )}
        </div>
        {isManager && (
          <Button
            size="sm"
            onClick={() => { setEditTarget(null); setFormOpen(true) }}
            style={{ background: 'var(--accent-500)', color: '#fff', borderRadius: 8, height: 34, fontSize: '0.8125rem', gap: 6 }}
          >
            <Plus size={14} /> New Warehouse
          </Button>
        )}
      </div>

      {/* Table */}
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-default)' }}>
              {['Name', 'Code', 'City', 'Country', 'Status', ''].map((h) => (
                <th
                  key={h}
                  style={{
                    padding: '10px 16px',
                    textAlign: h === '' ? 'right' : 'left',
                    fontSize: '0.75rem', fontWeight: 600,
                    letterSpacing: '0.05em', textTransform: 'uppercase',
                    color: 'var(--text-secondary)',
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 4 }).map((_, i) => (
                <tr key={i} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                  {[180, 100, 120, 80, 80, 60].map((w, j) => (
                    <td key={j} style={{ padding: '14px 16px' }}>
                      <div style={{ height: 14, width: w, background: 'var(--bg-elevated)', borderRadius: 4 }} />
                    </td>
                  ))}
                </tr>
              ))
            ) : warehouses.length === 0 ? (
              <tr>
                <td colSpan={6}>
                  <div style={{ textAlign: 'center', padding: '48px 24px' }}>
                    <p style={{ fontSize: '2rem', marginBottom: 8 }}>🏭</p>
                    <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>No warehouses yet.</p>
                  </div>
                </td>
              </tr>
            ) : (
              warehouses.map((w, idx) => (
                <tr
                  key={w.id}
                  style={{
                    borderBottom: '1px solid var(--border-subtle)',
                    background: idx % 2 === 0 ? 'transparent' : 'var(--bg-surface)',
                    transition: 'background 0.15s',
                  }}
                  className="hover:bg-[var(--bg-elevated)]"
                >
                  <td style={{ padding: '13px 16px' }}>
                    <p style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-primary)' }}>{w.name}</p>
                  </td>
                  <td style={{ padding: '13px 16px' }}>
                    <code style={{ fontSize: '0.8125rem', fontFamily: 'JetBrains Mono, monospace', color: 'var(--accent-400)', background: 'var(--bg-elevated)', padding: '2px 8px', borderRadius: 4 }}>
                      {w.code}
                    </code>
                  </td>
                  <td style={{ padding: '13px 16px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{w.city ?? '—'}</td>
                  <td style={{ padding: '13px 16px', fontSize: '0.875rem', color: 'var(--text-secondary)' }}>{w.country ?? '—'}</td>
                  <td style={{ padding: '13px 16px' }}>
                    {w.active ? (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, color: 'var(--success)', fontSize: '0.8125rem', fontWeight: 500 }}>
                        <CheckCircle size={13} /> Active
                      </span>
                    ) : (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, color: 'var(--text-muted)', fontSize: '0.8125rem', fontWeight: 500 }}>
                        <XCircle size={13} /> Inactive
                      </span>
                    )}
                  </td>
                  <td style={{ padding: '13px 16px', textAlign: 'right' }}>
                    {isManager && (
                      <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end' }}>
                        <Button
                          variant="ghost" size="sm"
                          onClick={() => { setEditTarget(w); setFormOpen(true) }}
                          style={{ color: 'var(--text-muted)', padding: '4px 8px' }}
                        >
                          <Pencil size={14} />
                        </Button>
                        {isAdmin && (
                          <Button
                            variant="ghost" size="sm"
                            onClick={() => setDeleteTarget(w)}
                            style={{ color: 'var(--error)', padding: '4px 8px' }}
                          >
                            <Trash2 size={14} />
                          </Button>
                        )}
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Dialogs */}
      <WarehouseFormDialog
        warehouse={editTarget}
        open={formOpen}
        onClose={() => { setFormOpen(false); setEditTarget(null) }}
      />
      <DeleteConfirmDialog
        warehouse={deleteTarget}
        onClose={() => setDeleteTarget(null)}
      />
    </div>
  )
}
