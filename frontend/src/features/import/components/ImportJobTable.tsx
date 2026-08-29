/**
 * features/import/components/ImportJobTable.tsx
 * Paginated table of import job summaries.
 */
import { useNavigate } from 'react-router-dom'
import { Eye, FileSpreadsheet } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { TableSkeleton } from '@/components/common/TableSkeleton'
import { ImportJobStatusBadge } from './ImportJobStatusBadge'
import type { ImportJobSummaryResponse } from '../types/import.types'

const TYPE_LABELS: Record<string, string> = {
  PRODUCT:  'Products',
  CUSTOMER: 'Customers',
  ORDER:    'Orders',
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('en-GB', {
    dateStyle: 'short',
    timeStyle: 'short',
  })
}

interface ImportJobTableProps {
  jobs: ImportJobSummaryResponse[]
  isLoading: boolean
}

export function ImportJobTable({ jobs, isLoading }: ImportJobTableProps) {
  const navigate = useNavigate()

  if (isLoading) return <TableSkeleton rows={6} cols={7} />

  if (jobs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 gap-3">
        <div
          className="flex h-14 w-14 items-center justify-center rounded-2xl"
          style={{ background: 'var(--bg-overlay)' }}
        >
          <FileSpreadsheet className="h-7 w-7" style={{ color: 'var(--text-muted)' }} />
        </div>
        <p className="text-body-sm font-medium" style={{ color: 'var(--text-secondary)' }}>
          No import jobs yet
        </p>
        <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
          Upload a CSV or XLSX file to get started
        </p>
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-xl border" style={{ borderColor: 'var(--border-default)' }}>
      <table className="w-full text-body-sm">
        <thead>
          <tr style={{ background: 'var(--bg-overlay)', borderBottom: '1px solid var(--border-subtle)' }}>
            {['Type', 'File Name', 'Status', 'Total', 'Successful', 'Failed', 'Created', ''].map(
              (h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left text-caption font-semibold tracking-wide whitespace-nowrap"
                  style={{ color: 'var(--text-muted)' }}
                >
                  {h}
                </th>
              )
            )}
          </tr>
        </thead>
        <tbody>
          {jobs.map((job, idx) => (
            <tr
              key={job.id}
              className="border-b transition-colors hover:bg-[rgba(99,102,241,0.04)]"
              style={{
                borderColor: 'var(--border-subtle)',
                background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
              }}
            >
              <td className="px-4 py-3 whitespace-nowrap">
                <span
                  className="text-caption font-medium px-2 py-0.5 rounded"
                  style={{ background: 'var(--bg-overlay)', color: 'var(--text-secondary)' }}
                >
                  {TYPE_LABELS[job.importType] ?? job.importType}
                </span>
              </td>
              <td
                className="px-4 py-3 max-w-[220px] truncate font-medium"
                style={{ color: 'var(--text-primary)' }}
                title={job.fileName}
              >
                {job.fileName}
              </td>
              <td className="px-4 py-3 whitespace-nowrap">
                <ImportJobStatusBadge status={job.status} />
              </td>
              <td className="px-4 py-3 tabular-nums" style={{ color: 'var(--text-secondary)' }}>
                {job.totalRows.toLocaleString()}
              </td>
              <td className="px-4 py-3 tabular-nums" style={{ color: 'var(--success)' }}>
                {job.successfulRows.toLocaleString()}
              </td>
              <td
                className="px-4 py-3 tabular-nums"
                style={{ color: job.failedRows > 0 ? 'var(--error)' : 'var(--text-muted)' }}
              >
                {job.failedRows.toLocaleString()}
              </td>
              <td className="px-4 py-3 whitespace-nowrap" style={{ color: 'var(--text-muted)' }}>
                {formatDate(job.createdAt)}
              </td>
              <td className="px-4 py-3">
                <Button
                  id={`view-job-${job.id}`}
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate(`/import/jobs/${job.id}`)}
                  className="gap-1.5"
                  style={{ color: 'var(--accent-400)' }}
                >
                  <Eye className="h-3.5 w-3.5" />
                  View
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
