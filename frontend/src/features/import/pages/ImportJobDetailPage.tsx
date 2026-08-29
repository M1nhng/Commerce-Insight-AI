/**
 * features/import/pages/ImportJobDetailPage.tsx
 *
 * Route: /import/jobs/:id
 *
 * Shows:
 *  - Job metadata (file, type, status, created at)
 *  - ImportProgress — lifecycle state with indeterminate indicator
 *  - ImportJobStats — total / successful / failed counters
 *  - ImportErrorTable — paginated per-row errors (shown when failedRows > 0)
 */
import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ArrowLeft, FileSpreadsheet, Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { CardSkeleton } from '@/components/common/TableSkeleton'
import { ImportJobStatusBadge } from '../components/ImportJobStatusBadge'
import { ImportJobStats } from '../components/ImportJobStats'
import { ImportProgress } from '../components/ImportProgress'
import { ImportErrorTable } from '../components/ImportErrorTable'
import { useImportJob, useImportJobErrors } from '../hooks/useImportJobs'
import type { ImportErrorFilterParams } from '../types/import.types'

const TYPE_LABELS: Record<string, string> = {
  PRODUCT:  'Products',
  CUSTOMER: 'Customers',
  ORDER:    'Orders',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' })
}

// ── Info row helper ───────────────────────────────────────────────────────

function InfoRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5 border-b last:border-0"
         style={{ borderColor: 'var(--border-subtle)' }}>
      <span className="text-caption font-medium shrink-0" style={{ color: 'var(--text-muted)' }}>
        {label}
      </span>
      <span className="text-body-sm font-medium text-right" style={{ color: 'var(--text-primary)' }}>
        {value}
      </span>
    </div>
  )
}

// ── Main Component ────────────────────────────────────────────────────────

export function ImportJobDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  // Error table pagination
  const [errPage, setErrPage] = useState(0)
  const [errSize] = useState(20)

  const errorParams: ImportErrorFilterParams = { page: errPage, size: errSize }

  const { data: job, isLoading } = useImportJob(id ?? null)
  const showErrors = !!(job && job.failedRows > 0)
  const { data: errorsPage, isLoading: errorsLoading } = useImportJobErrors(
    showErrors ? (id ?? null) : null,
    errorParams
  )

  // ── Loading ───────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto animate-fade-in">
        <CardSkeleton className="h-16" />
        <CardSkeleton className="h-32" />
        <CardSkeleton className="h-24" />
        <CardSkeleton className="h-64" />
      </div>
    )
  }

  // ── Not Found ─────────────────────────────────────────────────────────

  if (!job) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-4 animate-fade-in">
        <div
          className="flex h-16 w-16 items-center justify-center rounded-2xl"
          style={{ background: 'var(--bg-overlay)' }}
        >
          <FileSpreadsheet className="h-8 w-8" style={{ color: 'var(--text-muted)' }} />
        </div>
        <div className="text-center">
          <p className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
            Import job not found
          </p>
          <p className="text-caption mt-1" style={{ color: 'var(--text-muted)' }}>
            The job may have been removed or the ID is invalid.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/import/jobs')}
          style={{ borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
        >
          Back to History
        </Button>
      </div>
    )
  }

  // ── Detail view ───────────────────────────────────────────────────────

  return (
    <div className="space-y-6 max-w-3xl mx-auto animate-fade-in">
      {/* ── Back + Header ─────────────────────────────────────────────── */}
      <div>
        <Button
          id="back-to-jobs-btn"
          variant="ghost"
          size="sm"
          onClick={() => navigate(-1)}
          className="mb-4 gap-2 -ml-2"
          style={{ color: 'var(--text-muted)' }}
        >
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-heading-2 font-bold" style={{ color: 'var(--text-primary)' }}>
              Import Job
            </h1>
            <p
              className="text-caption font-mono mt-0.5"
              style={{ color: 'var(--text-muted)' }}
            >
              {job.id}
            </p>
          </div>
          <ImportJobStatusBadge status={job.status} />
        </div>
      </div>

      {/* ── Progress ──────────────────────────────────────────────────── */}
      <ImportProgress status={job.status} />

      {/* ── Job Details ───────────────────────────────────────────────── */}
      <div
        className="rounded-xl border px-5 py-2"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <InfoRow label="File" value={
          <span className="flex items-center gap-1.5">
            <FileSpreadsheet className="h-3.5 w-3.5 shrink-0" style={{ color: 'var(--accent-400)' }} />
            {job.fileName}
          </span>
        } />
        <InfoRow label="File Type" value={job.fileType} />
        <InfoRow label="Import Type" value={TYPE_LABELS[job.importType] ?? job.importType} />
        <InfoRow label="Created At" value={formatDate(job.createdAt)} />
        {job.startedAt && <InfoRow label="Started At" value={formatDate(job.startedAt)} />}
        {job.completedAt && <InfoRow label="Completed At" value={formatDate(job.completedAt)} />}
        {job.createdByEmail && <InfoRow label="Uploaded By" value={job.createdByEmail} />}
      </div>

      {/* ── Statistics ────────────────────────────────────────────────── */}
      {job.totalRows > 0 && (
        <div className="space-y-3">
          <h2 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
            Statistics
          </h2>
          <ImportJobStats job={job} />
        </div>
      )}

      {/* ── Errors Section ────────────────────────────────────────────── */}
      <div className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
            Row Errors
            {showErrors && (
              <span
                className="ml-2 text-caption px-2 py-0.5 rounded-full"
                style={{ background: 'var(--error-bg)', color: 'var(--error)' }}
              >
                {job.failedRows.toLocaleString()}
              </span>
            )}
          </h2>
        </div>

        <div
          className="rounded-xl border"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
        >
          <ImportErrorTable
            errors={errorsPage?.content ?? []}
            isLoading={errorsLoading}
          />
        </div>

        {/* Error table pagination */}
        {errorsPage && errorsPage.totalPages > 1 && (
          <div className="flex items-center justify-end gap-2 mt-2">
            <Button
              variant="outline" size="sm"
              disabled={errPage === 0}
              onClick={() => setErrPage((p) => p - 1)}
              style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
            >← Prev</Button>
            <span className="text-caption" style={{ color: 'var(--text-muted)' }}>
              {errPage + 1} / {errorsPage.totalPages}
            </span>
            <Button
              variant="outline" size="sm"
              disabled={errPage >= errorsPage.totalPages - 1}
              onClick={() => setErrPage((p) => p + 1)}
              style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)', color: 'var(--text-secondary)' }}
            >Next →</Button>
          </div>
        )}
      </div>

      {/* ── Re-import CTA ─────────────────────────────────────────────── */}
      <div
        className="flex items-center justify-between rounded-xl border px-5 py-4"
        style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
      >
        <div>
          <p className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>
            Need to fix and re-import?
          </p>
          <p className="text-caption mt-0.5" style={{ color: 'var(--text-muted)' }}>
            Review the errors above, correct your file, and upload again.
          </p>
        </div>
        <Button
          id="go-to-import-btn"
          asChild
          style={{ background: 'var(--accent-500)', color: '#fff', border: 'none' }}
        >
          <Link to="/import">
            <Upload className="h-4 w-4 mr-2" />
            New Import
          </Link>
        </Button>
      </div>
    </div>
  )
}
