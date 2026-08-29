/**
 * features/import/components/ImportProgress.tsx
 * Lifecycle progress display for an import job.
 *
 * Uses indeterminate animations — never fakes percentages.
 * Shows contextual messaging per lifecycle state.
 */
import { CheckCircle2, XCircle, AlertTriangle, Loader2 } from 'lucide-react'
import type { ImportJobStatus } from '../types/import.types'

interface StateConfig {
  icon: React.ReactNode
  title: string
  description: string
  showBar: boolean
}

function getConfig(status: ImportJobStatus): StateConfig {
  switch (status) {
    case 'UPLOADED':
      return {
        icon: <Loader2 className="h-5 w-5 animate-spin" style={{ color: 'var(--text-muted)' }} />,
        title: 'Queued for processing',
        description: 'Your file has been received and is waiting to be processed.',
        showBar: true,
      }
    case 'VALIDATING':
      return {
        icon: <Loader2 className="h-5 w-5 animate-spin" style={{ color: 'var(--info)' }} />,
        title: 'Validating file…',
        description: 'Checking file headers and structure.',
        showBar: true,
      }
    case 'IMPORTING':
      return {
        icon: <Loader2 className="h-5 w-5 animate-spin" style={{ color: 'var(--accent-400)' }} />,
        title: 'Importing data…',
        description: 'Processing rows one by one. Errors will be recorded automatically.',
        showBar: true,
      }
    case 'COMPLETED':
      return {
        icon: <CheckCircle2 className="h-5 w-5" style={{ color: 'var(--success)' }} />,
        title: 'Import completed successfully',
        description: 'All rows were imported without errors.',
        showBar: false,
      }
    case 'PARTIAL_SUCCESS':
      return {
        icon: <AlertTriangle className="h-5 w-5" style={{ color: 'var(--warning)' }} />,
        title: 'Import completed with errors',
        description: 'Some rows failed. Review the error table below for details.',
        showBar: false,
      }
    case 'FAILED':
      return {
        icon: <XCircle className="h-5 w-5" style={{ color: 'var(--error)' }} />,
        title: 'Import failed',
        description: 'The file could not be processed. Check the error details below.',
        showBar: false,
      }
  }
}

interface ImportProgressProps {
  status: ImportJobStatus
}

export function ImportProgress({ status }: ImportProgressProps) {
  const config = getConfig(status)

  return (
    <div
      className="rounded-xl border px-5 py-4 space-y-3"
      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
    >
      <div className="flex items-center gap-3">
        {config.icon}
        <div>
          <p className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
            {config.title}
          </p>
          <p className="text-caption mt-0.5" style={{ color: 'var(--text-muted)' }}>
            {config.description}
          </p>
        </div>
      </div>

      {/* Indeterminate progress bar — only shown while processing */}
      {config.showBar && (
        <div
          className="h-1 w-full rounded-full overflow-hidden"
          style={{ background: 'var(--border-subtle)' }}
        >
          <div
            className="h-full rounded-full animate-[progress-indeterminate_1.5s_ease-in-out_infinite]"
            style={{
              width: '40%',
              background: 'linear-gradient(90deg, var(--accent-600), var(--accent-400))',
              animation: 'pulse 1.5s ease-in-out infinite',
            }}
          />
        </div>
      )}
    </div>
  )
}
