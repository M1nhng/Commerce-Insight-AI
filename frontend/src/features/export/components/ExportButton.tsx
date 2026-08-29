/**
 * features/export/components/ExportButton.tsx
 *
 * Primary action. While an export is running it shows a spinner + "Exporting…"
 * and is disabled, which also prevents a second concurrent request. No fake
 * percentage progress — the backend export is a single synchronous download.
 */
import { Download, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface Props {
  onExport: () => void
  isPending: boolean
  disabledReason?: string | null
}

export function ExportButton({ onExport, isPending, disabledReason }: Props) {
  const blocked = Boolean(disabledReason)

  return (
    <div className="space-y-2">
      <Button
        id="export-run-btn"
        type="button"
        onClick={onExport}
        disabled={isPending || blocked}
        aria-busy={isPending}
        aria-disabled={isPending || blocked}
        className="w-full sm:w-auto"
        style={{ background: 'var(--accent-500)', color: '#fff' }}
      >
        {isPending ? (
          <>
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
            Exporting…
          </>
        ) : (
          <>
            <Download className="h-4 w-4" aria-hidden />
            Export Report
          </>
        )}
      </Button>

      {isPending && (
        <p className="text-caption" style={{ color: 'var(--text-muted)' }} role="status">
          Generating your file. This may take a few seconds for large data sets.
        </p>
      )}

      {!isPending && blocked && (
        <p className="text-caption" style={{ color: 'var(--error)' }} role="alert">
          {disabledReason}
        </p>
      )}
    </div>
  )
}
