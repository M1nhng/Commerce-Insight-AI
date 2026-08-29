/**
 * features/import/components/ImportJobStats.tsx
 * Three stat cards: Total / Successful / Failed rows.
 */
import type { ImportJobResponse } from '../types/import.types'

interface StatCardProps {
  label: string
  value: number
  color: string
  bgColor: string
}

function StatCard({ label, value, color, bgColor }: StatCardProps) {
  return (
    <div
      className="flex-1 rounded-xl border px-5 py-4 min-w-[120px]"
      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
    >
      <p className="text-caption font-medium" style={{ color: 'var(--text-muted)' }}>
        {label}
      </p>
      <p
        className="text-heading-2 font-bold mt-1 tabular-nums"
        style={{ color }}
      >
        {value.toLocaleString()}
      </p>
      <div
        className="mt-3 h-1 rounded-full"
        style={{ background: bgColor, opacity: 0.4 }}
      />
    </div>
  )
}

interface ImportJobStatsProps {
  job: ImportJobResponse
}

export function ImportJobStats({ job }: ImportJobStatsProps) {
  return (
    <div className="flex gap-3 flex-wrap">
      <StatCard
        label="Total Rows"
        value={job.totalRows}
        color="var(--text-primary)"
        bgColor="var(--border-default)"
      />
      <StatCard
        label="Successful"
        value={job.successfulRows}
        color="var(--success)"
        bgColor="var(--success)"
      />
      <StatCard
        label="Failed"
        value={job.failedRows}
        color={job.failedRows > 0 ? 'var(--error)' : 'var(--text-muted)'}
        bgColor={job.failedRows > 0 ? 'var(--error)' : 'var(--border-default)'}
      />
    </div>
  )
}
