/**
 * features/import/components/ImportJobStatusBadge.tsx
 * Status badge for import job lifecycle states.
 */
import { cn } from '@/lib/utils'
import type { ImportJobStatus } from '../types/import.types'

interface Config {
  label: string
  className: string
  style: React.CSSProperties
}

const STATUS_CONFIG: Record<ImportJobStatus, Config> = {
  UPLOADED: {
    label: 'Uploaded',
    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md',
    style: { background: 'var(--bg-overlay)', color: 'var(--text-muted)' },
  },
  VALIDATING: {
    label: 'Validating',
    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md',
    style: { background: 'var(--info-bg)', color: 'var(--info)' },
  },
  IMPORTING: {
    label: 'Importing',
    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md',
    style: { background: 'var(--info-bg)', color: 'var(--accent-400)' },
  },
  COMPLETED: {
    label: 'Completed',
    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md',
    style: { background: 'var(--success-bg)', color: 'var(--success)' },
  },
  PARTIAL_SUCCESS: {
    label: 'Partial',
    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md',
    style: { background: 'var(--warning-bg)', color: 'var(--warning)' },
  },
  FAILED: {
    label: 'Failed',
    className: 'font-medium text-caption uppercase tracking-wide px-2.5 py-1 rounded-md',
    style: { background: 'var(--error-bg)', color: 'var(--error)' },
  },
}

interface ImportJobStatusBadgeProps {
  status: ImportJobStatus
  className?: string
}

export function ImportJobStatusBadge({ status, className }: ImportJobStatusBadgeProps) {
  const config = STATUS_CONFIG[status] ?? STATUS_CONFIG.FAILED
  return (
    <span className={cn(config.className, className)} style={config.style}>
      {config.label}
    </span>
  )
}
