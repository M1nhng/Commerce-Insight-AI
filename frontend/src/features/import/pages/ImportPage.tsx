/**
 * features/import/pages/ImportPage.tsx
 *
 * Route: /import
 *
 * Step-by-step import flow:
 *   1. Select import type (all authenticated users can view)
 *   2. Select file via dropzone  (MANAGER / ADMIN only)
 *   3. Preview + start import    (MANAGER / ADMIN only)
 *
 * STAFF see the type selector and template download only.
 */
import { useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { Upload, History } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ImportTypeSelector } from '../components/ImportTypeSelector'
import { ImportDropzone } from '../components/ImportDropzone'
import { ImportFilePreview } from '../components/ImportFilePreview'
import { useUploadImport, useDownloadTemplate } from '../hooks/useImport'
import { useAuth } from '@/hooks/useAuth'
import type { ImportType } from '../types/import.types'

export function ImportPage() {
  const { isAtLeast } = useAuth()
  const canUpload = isAtLeast('MANAGER')

  const [selectedType, setSelectedType] = useState<ImportType | null>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)

  const upload = useUploadImport()
  const downloadTemplate = useDownloadTemplate()

  const handleTypeChange = useCallback((type: ImportType) => {
    setSelectedType(type)
    setSelectedFile(null)          // reset file when type changes
  }, [])

  const handleFileSelected = useCallback((file: File) => {
    setSelectedFile(file)
  }, [])

  const handleRemoveFile = useCallback(() => {
    setSelectedFile(null)
  }, [])

  const handleStartImport = useCallback(() => {
    if (!selectedType || !selectedFile) return
    upload.mutate({ type: selectedType, file: selectedFile })
  }, [selectedType, selectedFile, upload])

  return (
    <div className="space-y-6 max-w-2xl mx-auto animate-fade-in">
      {/* ── Page Header ──────────────────────────────────────────────── */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-heading-2 font-bold" style={{ color: 'var(--text-primary)' }}>
            Import Data
          </h1>
          <p className="text-body-sm mt-1" style={{ color: 'var(--text-secondary)' }}>
            Import Products, Customers, or Orders using CSV or Excel files.
          </p>
        </div>
        <Button
          id="view-import-history-btn"
          variant="outline"
          size="sm"
          asChild
          style={{
            background: 'var(--bg-elevated)',
            borderColor: 'var(--border-default)',
            color: 'var(--text-secondary)',
          }}
        >
          <Link to="/import/jobs">
            <History className="h-4 w-4 mr-2" />
            Import History
          </Link>
        </Button>
      </div>

      {/* ── Step 1: Select Import Type ────────────────────────────────── */}
      <div
        className="rounded-xl border p-5 space-y-4"
        style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
      >
        <div className="flex items-center gap-2">
          <span
            className="flex h-6 w-6 items-center justify-center rounded-full text-caption font-bold"
            style={{ background: 'var(--accent-500)', color: '#fff' }}
          >
            1
          </span>
          <h2 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
            Select Data Type
          </h2>
        </div>
        <ImportTypeSelector
          value={selectedType}
          onChange={handleTypeChange}
          disabled={upload.isPending}
        />

        {/* Template download for STAFF */}
        {selectedType && !canUpload && (
          <div
            className="flex items-center justify-between rounded-lg px-4 py-3 mt-2"
            style={{ background: 'var(--bg-overlay)', border: '1px solid var(--border-subtle)' }}
          >
            <div>
              <p className="text-body-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                Download Template
              </p>
              <p className="text-caption" style={{ color: 'var(--text-muted)' }}>
                Use the template to prepare your data for import.
              </p>
            </div>
            <Button
              id="staff-download-template-btn"
              variant="outline"
              size="sm"
              disabled={downloadTemplate.isPending}
              onClick={() => downloadTemplate.mutate(selectedType)}
              style={{
                background: 'var(--bg-elevated)',
                borderColor: 'var(--border-default)',
                color: 'var(--text-secondary)',
              }}
            >
              Download
            </Button>
          </div>
        )}
      </div>

      {/* ── Step 2: Select File (MANAGER / ADMIN only) ────────────────── */}
      {canUpload && selectedType && !selectedFile && (
        <div
          className="rounded-xl border p-5 space-y-4 animate-fade-in"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
        >
          <div className="flex items-center gap-2">
            <span
              className="flex h-6 w-6 items-center justify-center rounded-full text-caption font-bold"
              style={{ background: 'var(--accent-500)', color: '#fff' }}
            >
              2
            </span>
            <h2 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
              Select File
            </h2>
          </div>
          <ImportDropzone
            onFileSelected={handleFileSelected}
            disabled={upload.isPending}
          />
        </div>
      )}

      {/* ── Step 3: Preview & Upload (MANAGER / ADMIN only) ──────────── */}
      {canUpload && selectedType && selectedFile && (
        <div
          className="rounded-xl border p-5 space-y-4 animate-fade-in"
          style={{ background: 'var(--bg-surface)', borderColor: 'var(--border-default)' }}
        >
          <div className="flex items-center gap-2">
            <span
              className="flex h-6 w-6 items-center justify-center rounded-full text-caption font-bold"
              style={{ background: 'var(--accent-500)', color: '#fff' }}
            >
              2
            </span>
            <h2 className="text-body-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
              Review & Start
            </h2>
          </div>
          <ImportFilePreview
            file={selectedFile}
            importType={selectedType}
            onRemove={handleRemoveFile}
            onStartImport={handleStartImport}
            isUploading={upload.isPending}
          />
        </div>
      )}

      {/* ── Role hint for STAFF ───────────────────────────────────────── */}
      {!canUpload && (
        <div
          className="flex items-start gap-3 rounded-xl border px-4 py-4"
          style={{ background: 'var(--bg-elevated)', borderColor: 'var(--border-default)' }}
        >
          <Upload className="h-4 w-4 mt-0.5 shrink-0" style={{ color: 'var(--text-muted)' }} />
          <div>
            <p className="text-body-sm font-medium" style={{ color: 'var(--text-secondary)' }}>
              Upload access required
            </p>
            <p className="text-caption mt-0.5" style={{ color: 'var(--text-muted)' }}>
              File uploads are available to Manager and Admin roles.
              You can download templates and view import history.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
