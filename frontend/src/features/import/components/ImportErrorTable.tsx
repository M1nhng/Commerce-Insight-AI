/**
 * features/import/components/ImportErrorTable.tsx
 * Paginated table of per-row import errors for a specific job.
 */
import { AlertCircle } from 'lucide-react'
import { TableSkeleton } from '@/components/common/TableSkeleton'
import type { ImportErrorResponse } from '../types/import.types'

interface ImportErrorTableProps {
  errors: ImportErrorResponse[]
  isLoading: boolean
}

export function ImportErrorTable({ errors, isLoading }: ImportErrorTableProps) {
  if (isLoading) return <TableSkeleton rows={5} cols={5} />

  if (errors.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-10 gap-3">
        <div
          className="flex h-12 w-12 items-center justify-center rounded-xl"
          style={{ background: 'var(--success-bg)' }}
        >
          <AlertCircle className="h-6 w-6" style={{ color: 'var(--success)' }} />
        </div>
        <p className="text-body-sm font-medium" style={{ color: 'var(--text-secondary)' }}>
          No errors found for this import
        </p>
      </div>
    )
  }

  return (
    <div className="overflow-x-auto rounded-xl border" style={{ borderColor: 'var(--border-default)' }}>
      <table className="w-full text-body-sm">
        <thead>
          <tr style={{ background: 'var(--bg-overlay)', borderBottom: '1px solid var(--border-subtle)' }}>
            {['Row', 'Field', 'Error Code', 'Message', 'Raw Value'].map((h) => (
              <th
                key={h}
                className="px-4 py-3 text-left text-caption font-semibold tracking-wide whitespace-nowrap"
                style={{ color: 'var(--text-muted)' }}
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {errors.map((err, idx) => (
            <tr
              key={err.id}
              className="border-b"
              style={{
                borderColor: 'var(--border-subtle)',
                background: idx % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
              }}
            >
              <td className="px-4 py-3 tabular-nums font-mono text-caption"
                  style={{ color: 'var(--text-secondary)' }}>
                #{err.rowNumber}
              </td>
              <td className="px-4 py-3">
                {err.fieldName ? (
                  <code
                    className="text-caption px-1.5 py-0.5 rounded"
                    style={{ background: 'var(--bg-overlay)', color: 'var(--accent-400)' }}
                  >
                    {err.fieldName}
                  </code>
                ) : (
                  <span style={{ color: 'var(--text-muted)' }}>—</span>
                )}
              </td>
              <td className="px-4 py-3">
                <span
                  className="text-caption font-medium px-2 py-0.5 rounded"
                  style={{ background: 'var(--error-bg)', color: 'var(--error)' }}
                >
                  {err.errorCode}
                </span>
              </td>
              <td className="px-4 py-3 max-w-[300px]" style={{ color: 'var(--text-secondary)' }}>
                {err.errorMessage}
              </td>
              <td className="px-4 py-3 max-w-[160px] truncate font-mono text-caption"
                  style={{ color: 'var(--text-muted)' }}
                  title={err.rawValue ?? undefined}>
                {err.rawValue ?? '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
