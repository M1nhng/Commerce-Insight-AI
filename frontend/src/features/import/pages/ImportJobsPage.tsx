/**
 * features/import/pages/ImportJobsPage.tsx
 *
 * Route: /import/jobs
 *
 * Paginated import history with filters for import type and status.
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'
import { ImportJobTable } from '../components/ImportJobTable'
import { useImportJobs } from '../hooks/useImportJobs'
import { useAuth } from '@/hooks/useAuth'
import type { ImportType, ImportJobStatus, ImportJobFilterParams } from '../types/import.types'

// ── Pagination component (reuses same pattern as CustomersPage) ───────────

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
        of <strong style={{ color: 'var(--text-primary)' }}>{totalElements}</strong> jobs
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
                  : { background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }
                }
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

// ── Main Component ────────────────────────────────────────────────────────

const EMPTY_TYPE = '__all__' as const
const EMPTY_STATUS = '__all__' as const

export function ImportJobsPage() {
  const { isAtLeast } = useAuth()
  const canUpload = isAtLeast('MANAGER')

  // ── Filters ──────────────────────────────────────────────────────────────
  const [typeFilter, setTypeFilter] = useState<ImportType | typeof EMPTY_TYPE>(EMPTY_TYPE)
  const [statusFilter, setStatusFilter] = useState<ImportJobStatus | typeof EMPTY_STATUS>(EMPTY_STATUS)
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)

  const params: ImportJobFilterParams = {
    ...(typeFilter !== EMPTY_TYPE && { importType: typeFilter }),
    ...(statusFilter !== EMPTY_STATUS && { status: statusFilter }),
    page,
    size,
  }

  const { data: jobsPage, isLoading } = useImportJobs(params)

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ── Page Header ──────────────────────────────────────────────── */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-2 font-bold" style={{ color: 'var(--text-primary)' }}>
            Import History
          </h1>
          <p className="text-body-sm mt-1" style={{ color: 'var(--text-secondary)' }}>
            Track and review all previous import jobs.
          </p>
        </div>
        {canUpload && (
          <Button
            id="new-import-btn"
            asChild
            style={{ background: 'var(--accent-500)', color: '#fff', border: 'none' }}
          >
            <Link to="/import">
              <Plus className="h-4 w-4 mr-2" />
              New Import
            </Link>
          </Button>
        )}
      </div>

      {/* ── Filters ──────────────────────────────────────────────────── */}
      <div
        className="rounded-xl border p-4 flex flex-wrap items-center gap-3"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        {/* Type Filter */}
        <Select
          value={typeFilter}
          onValueChange={(v) => { setTypeFilter(v as ImportType | typeof EMPTY_TYPE); setPage(0) }}
        >
          <SelectTrigger
            id="filter-type"
            className="w-44 h-9 text-body-sm"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue placeholder="All Types" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value={EMPTY_TYPE} style={{ color: 'var(--text-primary)' }}>All Types</SelectItem>
            <SelectItem value="PRODUCT"  style={{ color: 'var(--text-primary)' }}>Products</SelectItem>
            <SelectItem value="CUSTOMER" style={{ color: 'var(--text-primary)' }}>Customers</SelectItem>
            <SelectItem value="ORDER"    style={{ color: 'var(--text-primary)' }}>Orders</SelectItem>
          </SelectContent>
        </Select>

        {/* Status Filter */}
        <Select
          value={statusFilter}
          onValueChange={(v) => { setStatusFilter(v as ImportJobStatus | typeof EMPTY_STATUS); setPage(0) }}
        >
          <SelectTrigger
            id="filter-status"
            className="w-44 h-9 text-body-sm"
            style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
          >
            <SelectValue placeholder="All Statuses" />
          </SelectTrigger>
          <SelectContent style={{ background: 'var(--bg-elevated)', border: '1px solid var(--border-default)' }}>
            <SelectItem value={EMPTY_STATUS} style={{ color: 'var(--text-primary)' }}>All Statuses</SelectItem>
            <SelectItem value="COMPLETED"      style={{ color: 'var(--text-primary)' }}>Completed</SelectItem>
            <SelectItem value="PARTIAL_SUCCESS" style={{ color: 'var(--text-primary)' }}>Partial Success</SelectItem>
            <SelectItem value="FAILED"         style={{ color: 'var(--text-primary)' }}>Failed</SelectItem>
            <SelectItem value="IMPORTING"      style={{ color: 'var(--text-primary)' }}>Importing</SelectItem>
            <SelectItem value="VALIDATING"     style={{ color: 'var(--text-primary)' }}>Validating</SelectItem>
            <SelectItem value="UPLOADED"       style={{ color: 'var(--text-primary)' }}>Uploaded</SelectItem>
          </SelectContent>
        </Select>

        {/* Clear filters */}
        {(typeFilter !== EMPTY_TYPE || statusFilter !== EMPTY_STATUS) && (
          <Button
            id="clear-filters-btn"
            variant="ghost"
            size="sm"
            onClick={() => { setTypeFilter(EMPTY_TYPE); setStatusFilter(EMPTY_STATUS); setPage(0) }}
            style={{ color: 'var(--text-muted)' }}
          >
            Clear filters
          </Button>
        )}
      </div>

      {/* ── Table ────────────────────────────────────────────────────── */}
      <div
        className="rounded-xl border"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <ImportJobTable
          jobs={jobsPage?.content ?? []}
          isLoading={isLoading}
        />
      </div>

      {/* ── Pagination ────────────────────────────────────────────────── */}
      {jobsPage && jobsPage.totalPages > 0 && (
        <Pagination
          page={page}
          totalPages={jobsPage.totalPages}
          totalElements={jobsPage.totalElements}
          size={size}
          onPageChange={setPage}
          onSizeChange={setSize}
        />
      )}
    </div>
  )
}
