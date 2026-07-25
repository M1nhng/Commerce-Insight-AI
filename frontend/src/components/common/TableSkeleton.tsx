/**
 * components/common/TableSkeleton.tsx
 * Loading skeleton for data tables — per design spec §10.4
 */
interface TableSkeletonProps {
  rows?: number
  cols?: number
}

export function TableSkeleton({ rows = 5, cols = 6 }: TableSkeletonProps) {
  return (
    <div className="animate-fade-in">
      {/* Header skeleton */}
      <div
        className="flex gap-4 px-4 py-3 border-b"
        style={{ borderColor: 'var(--border-subtle)' }}
      >
        {Array.from({ length: cols }).map((_, i) => (
          <div
            key={i}
            className="skeleton h-3 rounded"
            style={{ width: i === 0 ? '40px' : `${80 + i * 20}px`, flexShrink: 0 }}
          />
        ))}
      </div>
      {/* Row skeletons */}
      {Array.from({ length: rows }).map((_, row) => (
        <div
          key={row}
          className="flex items-center gap-4 px-4 py-4 border-b"
          style={{
            borderColor: 'var(--border-subtle)',
            background: row % 2 === 0 ? 'var(--bg-surface)' : 'var(--bg-elevated)',
          }}
        >
          {Array.from({ length: cols }).map((_, col) => (
            <div
              key={col}
              className="skeleton h-3 rounded"
              style={{
                width: col === 0 ? '16px' : `${60 + col * 25}px`,
                flexShrink: 0,
                opacity: 1 - col * 0.1,
              }}
            />
          ))}
        </div>
      ))}
    </div>
  )
}

/** Skeleton for a single card */
export function CardSkeleton({ className }: { className?: string }) {
  return (
    <div
      className={`skeleton rounded-xl p-6 ${className ?? ''}`}
      style={{ minHeight: 120 }}
    />
  )
}
