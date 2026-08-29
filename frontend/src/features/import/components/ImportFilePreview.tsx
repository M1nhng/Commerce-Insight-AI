/**
 * features/import/components/ImportFilePreview.tsx
 * Preview panel shown after a file is selected — step 3 of the import flow.
 *
 * Shows: file name, type badge, size, selected import type.
 * Actions: Download Template, Remove File, Start Import.
 */
import { FileSpreadsheet, Download, Trash2, Upload, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useDownloadTemplate } from '../hooks/useImport'
import type { ImportType } from '../types/import.types'

const TYPE_LABELS: Record<ImportType, string> = {
  PRODUCT:  'Products',
  CUSTOMER: 'Customers',
  ORDER:    'Orders',
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

interface ImportFilePreviewProps {
  file: File
  importType: ImportType
  onRemove: () => void
  onStartImport: () => void
  isUploading: boolean
}

export function ImportFilePreview({
  file,
  importType,
  onRemove,
  onStartImport,
  isUploading,
}: ImportFilePreviewProps) {
  const downloadTemplate = useDownloadTemplate()
  const ext = file.name.split('.').pop()?.toUpperCase() ?? 'CSV'

  return (
    <div
      className="rounded-xl border space-y-5 p-5"
      style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
    >
      {/* File Info */}
      <div className="flex items-start gap-4">
        <div
          className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl"
          style={{ background: 'rgba(99,102,241,0.12)' }}
        >
          <FileSpreadsheet className="h-6 w-6" style={{ color: 'var(--accent-400)' }} />
        </div>
        <div className="flex-1 min-w-0">
          <p
            className="text-body-sm font-semibold truncate"
            style={{ color: 'var(--text-primary)' }}
          >
            {file.name}
          </p>
          <div className="flex items-center gap-2 mt-1 flex-wrap">
            <span
              className="text-caption font-medium px-1.5 py-0.5 rounded"
              style={{ background: 'var(--bg-overlay)', color: 'var(--text-muted)' }}
            >
              {ext}
            </span>
            <span className="text-caption" style={{ color: 'var(--text-muted)' }}>
              {formatBytes(file.size)}
            </span>
          </div>
        </div>
      </div>

      {/* Import Type */}
      <div
        className="flex items-center justify-between rounded-lg px-3 py-2.5"
        style={{ background: 'var(--bg-overlay)', border: '1px solid var(--border-subtle)' }}
      >
        <span className="text-caption font-medium" style={{ color: 'var(--text-muted)' }}>
          Import Type
        </span>
        <span
          className="text-body-sm font-semibold"
          style={{ color: 'var(--accent-400)' }}
        >
          {TYPE_LABELS[importType]}
        </span>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3 flex-wrap">
        {/* Download Template */}
        <Button
          id="download-template-btn"
          variant="outline"
          size="sm"
          disabled={downloadTemplate.isPending || isUploading}
          onClick={() => downloadTemplate.mutate(importType)}
          className="gap-2"
          style={{
            background: 'var(--bg-surface)',
            borderColor: 'var(--border-default)',
            color: 'var(--text-secondary)',
          }}
        >
          {downloadTemplate.isPending ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Download className="h-3.5 w-3.5" />
          )}
          Download Template
        </Button>

        {/* Remove */}
        <Button
          id="remove-file-btn"
          variant="outline"
          size="sm"
          disabled={isUploading}
          onClick={onRemove}
          className="gap-2"
          style={{
            background: 'var(--bg-surface)',
            borderColor: 'var(--error)',
            color: 'var(--error)',
          }}
        >
          <Trash2 className="h-3.5 w-3.5" />
          Remove
        </Button>

        {/* Start Import — primary CTA */}
        <Button
          id="start-import-btn"
          disabled={isUploading}
          onClick={onStartImport}
          className="ml-auto gap-2"
          style={{
            background: 'var(--accent-500)',
            color: '#ffffff',
            border: 'none',
          }}
        >
          {isUploading ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              Uploading…
            </>
          ) : (
            <>
              <Upload className="h-4 w-4" />
              Start Import
            </>
          )}
        </Button>
      </div>
    </div>
  )
}
