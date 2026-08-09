/**
 * features/customers/pages/CustomersPage.tsx
 *
 * Main customer management page with search, filter, pagination, and CRUD.
 */
import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Search, X, SlidersHorizontal, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { CustomerTable } from '../components/CustomerTable'
import { useCustomers, useDeleteCustomer, useUpdateCustomerStatus } from '../hooks/useCustomers'
import { useCustomerGroupOptions } from '../hooks/useCustomerGroups'
import { useAuth } from '@/hooks/useAuth'
import type { CustomerSummaryResponse, CustomerFilterParams, CustomerStatus } from '@/types/customer.types'

// ── Pagination ────────────────────────────────────────────────────────────

function Pagination({
  page, totalPages, totalElements, size, onPageChange, onSizeChange,
}: {
  page: number; totalPages: number; totalElements: number
  size: number; onPageChange: (p: number) => void; onSizeChange: (s: number) => void
}) {
  const start = page * size + 1
  const end = Math.min((page + 1) * size, totalElements)

  return (
    <div className="flex items-center justify-between mt-4 flex-wrap gap-3">
      <p className="text-body-sm" style={{ color: 'var(--text-secondary)' }}>
        Showing{' '}
        <strong style={{ color: 'var(--text-primary)' }}>
          {totalElements === 0 ? 0 : start}–{end}
        </strong>{' '}
        of <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> customers
      </p>

      <div className="flex items-center gap-3">
        <Select value={String(size)} onValueChange={(v) => { onSizeChange(Number(v)); onPageChange(0) }}>
          <SelectTrigger
            className="w-28 h-8 text-body-sm"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            {[10, 25, 50].map((s) => (
              <SelectItem key={s} value={String(s)} style={{ color: 'var(--text-primary)' }}>
                {s} / page
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <div className="flex items-center gap-1">
          <Button
            variant="outline" size="sm"
            onClick={() => onPageChange(page - 1)} disabled={page === 0}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >← Prev</Button>

          {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
            const p = totalPages <= 5 ? i : Math.max(0, Math.min(page - 2, totalPages - 5)) + i
            return (
              <Button
                key={p} variant={p === page ? 'default' : 'outline'} size="sm"
                onClick={() => onPageChange(p)}
                style={p === page
                  ? { background: 'var(--accent-500)', color: '#fff', border: 'none' }
                  : { background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
              >{p + 1}</Button>
            )
          })}

          <Button
            variant="outline" size="sm"
            onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >Next →</Button>
        </div>
      </div>
    </div>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────

export function CustomersPage() {
  const navigate = useNavigate()
  const { isAtLeast } = useAuth()
  const canWrite = isAtLeast('MANAGER')
  const canDelete = isAtLeast('ADMIN')

  // ── Filter state ─────────────────────────────────────────────────────
  const [search, setSearch]               = useState('')
  const [debouncedSearch, setDebounced]   = useState('')
  const [statusFilter, setStatusFilter]   = useState<string>('__all__')
  const [groupFilter, setGroupFilter]     = useState<string>('__all__')
  const [page, setPage]                   = useState(0)
  const [size, setSize]                   = useState(10)

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value)
    const t = setTimeout(() => { setDebounced(value); setPage(0) }, 400)
    return () => clearTimeout(t)
  }, [])

  const filters: CustomerFilterParams = {
    keyword:  debouncedSearch || undefined,
    status:   statusFilter !== '__all__' ? (statusFilter as CustomerStatus) : undefined,
    groupId:  groupFilter  !== '__all__' ? groupFilter : undefined,
    page,
    size,
    sortBy:   'createdAt',
    sortDir:  'desc',
  }

  // ── Data ─────────────────────────────────────────────────────────────
  const { data, isLoading }       = useCustomers(filters)
  const { data: groupOptions = [] } = useCustomerGroupOptions()

  // ── Mutations ─────────────────────────────────────────────────────────
  const deleteCustomer     = useDeleteCustomer()
  const updateStatus       = useUpdateCustomerStatus()

  // ── Delete dialog ─────────────────────────────────────────────────────
  const [deleteTarget, setDeleteTarget] = useState<CustomerSummaryResponse | null>(null)

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteCustomer.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
  }

  const handleStatusChange = (customer: CustomerSummaryResponse, status: CustomerStatus) => {
    updateStatus.mutate({ id: customer.id, data: { status } })
  }

  const clearFilters = () => {
    setSearch(''); setDebounced(''); setStatusFilter('__all__'); setGroupFilter('__all__'); setPage(0)
  }

  const hasFilters = debouncedSearch || statusFilter !== '__all__' || groupFilter !== '__all__'

  return (
    <div className="animate-fade-in space-y-6">
      {/* ── Page Header ──────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-1 flex items-center gap-2" style={{ color: 'var(--text-primary)' }}>
            <Users className="h-7 w-7" style={{ color: 'var(--accent-400)' }} />
            Customers
          </h1>
          <p className="mt-1 text-body-sm" style={{ color: 'var(--text-secondary)' }}>
            {data?.totalElements ?? 0} customers in your database
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => navigate('/customers/groups')}
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            Manage Groups
          </Button>
          {canWrite && (
            <Button
              onClick={() => navigate('/customers/new')}
              className="gap-2"
              style={{ background: 'var(--accent-500)', color: '#fff' }}
            >
              <Plus className="h-4 w-4" /> Add Customer
            </Button>
          )}
        </div>
      </div>

      {/* ── Filter Bar ───────────────────────────────────────────────── */}
      <div
        className="flex flex-wrap gap-3 p-4 rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search
            className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4"
            style={{ color: 'var(--text-muted)' }}
          />
          <Input
            value={search}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Search by name, code, email, phone..."
            className="pl-9"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-primary)' }}
          />
        </div>

        {/* Status filter */}
        <Select value={statusFilter} onValueChange={(v) => { setStatusFilter(v); setPage(0) }}>
          <SelectTrigger
            className="w-36"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SlidersHorizontal className="h-3.5 w-3.5 mr-2 shrink-0" style={{ color: 'var(--text-muted)' }} />
            <SelectValue placeholder="All Status" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value="__all__" style={{ color: 'var(--text-secondary)' }}>All Status</SelectItem>
            <SelectItem value="ACTIVE"   style={{ color: 'var(--success)' }}>Active</SelectItem>
            <SelectItem value="INACTIVE" style={{ color: 'var(--text-muted)' }}>Inactive</SelectItem>
            <SelectItem value="BLOCKED"  style={{ color: 'var(--error)' }}>Blocked</SelectItem>
          </SelectContent>
        </Select>

        {/* Group filter */}
        <Select value={groupFilter} onValueChange={(v) => { setGroupFilter(v); setPage(0) }}>
          <SelectTrigger
            className="w-44"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue placeholder="All Groups" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value="__all__" style={{ color: 'var(--text-secondary)' }}>All Groups</SelectItem>
            {groupOptions.map((g) => (
              <SelectItem key={g.value} value={g.value} style={{ color: 'var(--text-primary)' }}>
                {g.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {hasFilters && (
          <Button
            variant="ghost" size="sm" onClick={clearFilters}
            className="gap-1.5" style={{ color: 'var(--text-muted)' }}
          >
            <X className="h-3.5 w-3.5" /> Clear
          </Button>
        )}
      </div>

      {/* ── Table ────────────────────────────────────────────────────── */}
      <CustomerTable
        customers={data?.content ?? []}
        isLoading={isLoading}
        canWrite={canWrite}
        canDelete={canDelete}
        onDelete={setDeleteTarget}
        onStatusChange={handleStatusChange}
      />

      {/* ── Pagination ───────────────────────────────────────────────── */}
      {data && data.totalPages > 0 && (
        <Pagination
          page={page}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={setSize}
        />
      )}

      {/* ── Confirm Delete ───────────────────────────────────────────── */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(o) => !o && setDeleteTarget(null)}
        title="Delete Customer?"
        description={`Are you sure you want to delete "${deleteTarget?.fullName}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        loading={deleteCustomer.isPending}
        onConfirm={handleConfirmDelete}
      />
    </div>
  )
}
