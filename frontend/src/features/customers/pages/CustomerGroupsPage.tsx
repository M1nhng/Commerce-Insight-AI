/**
 * features/customers/pages/CustomerGroupsPage.tsx
 * Customer Group management — list, create, edit, delete.
 */
import { useState } from 'react'
import { Plus, MoreHorizontal, Pencil, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { TableSkeleton } from '@/components/common/TableSkeleton'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { CustomerStatusBadge } from '../components/CustomerStatusBadge'
import { CustomerGroupForm } from '../components/CustomerGroupForm'
import {
  useCustomerGroups, useCreateCustomerGroup,
  useUpdateCustomerGroup, useDeleteCustomerGroup,
} from '../hooks/useCustomerGroups'
import { useAuth } from '@/hooks/useAuth'
import type { CustomerGroupResponse } from '@/types/customer.types'

export function CustomerGroupsPage() {
  const { isAtLeast }   = useAuth()
  const canWrite        = isAtLeast('MANAGER')
  const canDelete       = isAtLeast('ADMIN')

  // ── Data ─────────────────────────────────────────────────────────────
  const { data, isLoading } = useCustomerGroups({ size: 100, sortBy: 'createdAt', sortDir: 'desc' })
  const groups = data?.content ?? []

  // ── Mutations ─────────────────────────────────────────────────────────
  const createGroup = useCreateCustomerGroup()
  const updateGroup = useUpdateCustomerGroup()
  const deleteGroup = useDeleteCustomerGroup()

  // ── Local state ───────────────────────────────────────────────────────
  const [formOpen, setFormOpen]       = useState(false)
  const [editingGroup, setEditingGroup] = useState<CustomerGroupResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CustomerGroupResponse | null>(null)

  const handleFormSubmit = (values: any) => {
    if (editingGroup) {
      updateGroup.mutate(
        { id: editingGroup.id, data: { name: values.name, description: values.description, status: values.status } },
        { onSuccess: () => { setFormOpen(false); setEditingGroup(null) } }
      )
    } else {
      createGroup.mutate(values, { onSuccess: () => setFormOpen(false) })
    }
  }

  const openEdit = (group: CustomerGroupResponse) => {
    setEditingGroup(group)
    setFormOpen(true)
  }

  const openCreate = () => {
    setEditingGroup(null)
    setFormOpen(true)
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteGroup.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
  }

  return (
    <div className="animate-fade-in space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-1" style={{ color: 'var(--text-primary)' }}>
            Customer Groups
          </h1>
          <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
            {groups.length} group{groups.length !== 1 ? 's' : ''} defined
          </p>
        </div>

        {canWrite && (
          <Button
            onClick={openCreate}
            className="gap-2"
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            <Plus className="h-4 w-4" /> New Group
          </Button>
        )}
      </div>

      {/* Table */}
      {isLoading ? (
        <TableSkeleton rows={5} cols={5} />
      ) : groups.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-20 rounded-xl border"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
        >
          <div className="text-5xl mb-4">🏷️</div>
          <h3 className="text-heading-3 mb-1" style={{ color: 'var(--text-primary)' }}>
            No groups yet
          </h3>
          <p className="text-body-sm mb-4" style={{ color: 'var(--text-secondary)' }}>
            Create your first customer group to get started.
          </p>
          {canWrite && (
            <Button onClick={openCreate} style={{ background: 'var(--accent-500)', color: '#fff' }}>
              <Plus className="h-4 w-4 mr-2" /> New Group
            </Button>
          )}
        </div>
      ) : (
        <div
          className="rounded-xl border overflow-hidden"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
        >
          <div className="overflow-x-auto">
            <table className="w-full text-body-sm">
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: 'var(--bg-elevated)' }}>
                  {['Code', 'Name', 'Description', 'Status', 'Created', ''].map((h) => (
                    <th
                      key={h}
                      className="px-4 py-3 text-left font-medium text-caption uppercase tracking-wide"
                      style={{ color: 'var(--text-muted)' }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {groups.map((g, idx) => (
                  <tr
                    key={g.id}
                    style={{
                      borderBottom: '1px solid var(--border-subtle)',
                      background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
                    }}
                  >
                    {/* Code */}
                    <td className="px-4 py-3 font-mono font-semibold" style={{ color: 'var(--accent-400)' }}>
                      {g.code}
                    </td>
                    {/* Name */}
                    <td className="px-4 py-3 font-medium" style={{ color: 'var(--text-primary)' }}>
                      {g.name}
                    </td>
                    {/* Description */}
                    <td className="px-4 py-3 max-w-xs truncate" style={{ color: 'var(--text-secondary)' }}>
                      {g.description ?? <span style={{ color: 'var(--text-muted)' }}>—</span>}
                    </td>
                    {/* Status */}
                    <td className="px-4 py-3">
                      <CustomerStatusBadge status={g.status} />
                    </td>
                    {/* Created */}
                    <td className="px-4 py-3 text-caption" style={{ color: 'var(--text-muted)' }}>
                      {new Date(g.createdAt).toLocaleDateString('en-GB', {
                        day: '2-digit', month: 'short', year: 'numeric',
                      })}
                    </td>
                    {/* Actions */}
                    <td className="px-4 py-3">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost" size="icon" className="h-7 w-7"
                            style={{ color: 'var(--text-muted)' }}
                          >
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end"
                          style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
                          {canWrite && (
                            <DropdownMenuItem
                              className="gap-2 cursor-pointer"
                              style={{ color: 'var(--text-primary)' }}
                              onClick={() => openEdit(g)}
                            >
                              <Pencil className="h-4 w-4" /> Edit
                            </DropdownMenuItem>
                          )}
                          {canDelete && (
                            <>
                              <DropdownMenuSeparator style={{ background: 'var(--border-subtle)' }} />
                              <DropdownMenuItem
                                className="gap-2 cursor-pointer"
                                style={{ color: 'var(--error)' }}
                                onClick={() => setDeleteTarget(g)}
                              >
                                <Trash2 className="h-4 w-4" /> Delete
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
      )}

      {/* Form dialog */}
      <CustomerGroupForm
        open={formOpen}
        onOpenChange={(o) => { setFormOpen(o); if (!o) setEditingGroup(null) }}
        group={editingGroup}
        isSubmitting={createGroup.isPending || updateGroup.isPending}
        onSubmit={handleFormSubmit}
      />

      {/* Confirm delete */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(o) => !o && setDeleteTarget(null)}
        title="Delete Customer Group?"
        description={`Delete "${deleteTarget?.name}"? Customers in this group will not be deleted, but will lose their group assignment.`}
        confirmLabel="Delete Group"
        variant="destructive"
        loading={deleteGroup.isPending}
        onConfirm={handleConfirmDelete}
      />
    </div>
  )
}
