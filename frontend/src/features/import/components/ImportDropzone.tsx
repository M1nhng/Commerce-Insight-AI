/**
 * features/import/components/ImportDropzone.tsx
 * Drag-and-drop file picker with client-side validation.
 *
 * Supported: CSV, XLSX
 * Rejected: any other extension, files > 10 MB, empty files
 *
 * Client validation is UX only — backend remains authoritative.
 */
import { useCallback, useRef, useState } from 'react'
import { CloudUpload, FileSpreadsheet, X, AlertCircle } from 'lucide-react'
import { cn } from '@/lib/utils'

const MAX_SIZE_MB = 10
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024
const ALLOWED_EXTENSIONS = ['csv', 'xlsx']

function validateFile(file: File): string | null {
  if (file.size === 0) return 'File is empty.'
  if (file.size > MAX_SIZE_BYTES) {
    return `File is too large (${(file.size / 1024 / 1024).toFixed(1)} MB). Maximum is ${MAX_SIZE_MB} MB.`
  }
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
    return `Unsupported file type ".${ext ?? '?'}". Only .csv and .xlsx files are accepted.`
  }
  return null
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

interface ImportDropzoneProps {
  onFileSelected: (file: File) => void
  disabled?: boolean
}

export function ImportDropzone({ onFileSelected, disabled }: ImportDropzoneProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [isDragging, setIsDragging] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<File | null>(null)

  const handleFile = useCallback(
    (file: File) => {
      const err = validateFile(file)
      if (err) {
        setError(err)
        setSelected(null)
        return
      }
      setError(null)
      setSelected(file)
      onFileSelected(file)
    },
    [onFileSelected]
  )

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault()
      setIsDragging(false)
      if (disabled) return
      const file = e.dataTransfer.files[0]
      if (file) handleFile(file)
    },
    [disabled, handleFile]
  )

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0]
      if (file) handleFile(file)
      // Reset input so same file can be re-selected after remove
      e.target.value = ''
    },
    [handleFile]
  )

  const handleRemove = () => {
    setSelected(null)
    setError(null)
  }

  // ── Selected file view ────────────────────────────────────────────────────
  if (selected) {
    const ext = selected.name.split('.').pop()?.toUpperCase() ?? ''
    return (
      <div
        className="flex items-center gap-4 rounded-xl border px-4 py-4"
        style={{
          background: 'var(--bg-elevated)',
          borderColor: 'var(--border-default)',
        }}
      >
        <div
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg"
          style={{ background: 'rgba(99,102,241,0.12)' }}
        >
          <FileSpreadsheet className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
        </div>
        <div className="flex-1 min-w-0">
          <p
            className="text-body-sm font-medium truncate"
            style={{ color: 'var(--text-primary)' }}
          >
            {selected.name}
          </p>
          <p className="text-caption mt-0.5" style={{ color: 'var(--text-muted)' }}>
            {ext} · {formatBytes(selected.size)}
          </p>
        </div>
        {!disabled && (
          <button
            onClick={handleRemove}
            className="shrink-0 rounded-md p-1 transition-opacity hover:opacity-70"
            style={{ color: 'var(--text-muted)' }}
            aria-label="Remove selected file"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>
    )
  }

  // ── Dropzone view ─────────────────────────────────────────────────────────
  return (
    <div className="space-y-2">
      <div
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-label="File drop area — click or drag file here"
        onDragOver={(e) => { e.preventDefault(); if (!disabled) setIsDragging(true) }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => !disabled && inputRef.current?.click()}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click() }}
        className={cn(
          'flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed',
          'px-6 py-10 text-center transition-all duration-200',
          disabled
            ? 'cursor-not-allowed opacity-50'
            : 'cursor-pointer hover:border-[var(--accent-400)] hover:bg-[rgba(99,102,241,0.04)]'
        )}
        style={{
          borderColor: isDragging ? 'var(--accent-400)' : 'var(--border-default)',
          background: isDragging ? 'rgba(99,102,241,0.06)' : 'var(--bg-elevated)',
        }}
      >
        <div
          className="flex h-14 w-14 items-center justify-center rounded-2xl"
          style={{ background: isDragging ? 'rgba(99,102,241,0.12)' : 'var(--bg-overlay)' }}
        >
          <CloudUpload
            className="h-7 w-7 transition-colors"
            style={{ color: isDragging ? 'var(--accent-400)' : 'var(--text-muted)' }}
          />
        </div>
        <div>
          <p className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>
            Drag & Drop your file here
          </p>
          <p className="text-caption mt-1" style={{ color: 'var(--text-muted)' }}>
            or{' '}
            <span className="font-medium" style={{ color: 'var(--accent-400)' }}>
              Browse Files
            </span>
          </p>
        </div>
        <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
          CSV / XLSX · Max {MAX_SIZE_MB} MB
        </p>
      </div>

      {error && (
        <div
          className="flex items-start gap-2 rounded-lg px-3 py-2.5"
          style={{ background: 'var(--error-bg)', border: '1px solid var(--error)' }}
        >
          <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" style={{ color: 'var(--error)' }} />
          <p className="text-caption" style={{ color: 'var(--error)' }}>
            {error}
          </p>
        </div>
      )}

      <input
        ref={inputRef}
        type="file"
        accept=".csv,.xlsx"
        className="sr-only"
        onChange={handleInputChange}
        aria-hidden
      />
    </div>
  )
}
