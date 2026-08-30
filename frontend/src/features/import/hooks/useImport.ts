/**
 * features/import/hooks/useImport.ts
 * Mutation hook for file upload with navigation on success.
 */
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { getErrorMessage } from '@/lib/apiError'
import { importService } from '../services/importService'
import { IMPORT_KEYS } from './useImportJobs'
import type { ImportType } from '../types/import.types'

export function useUploadImport() {
  const qc = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: ({ type, file }: { type: ImportType; file: File }) =>
      importService.upload(type, file),

    onSuccess: (response) => {
      const job = response.data
      if (!job) return

      // Invalidate job list so it refreshes
      qc.invalidateQueries({ queryKey: IMPORT_KEYS.all })

      toast.success(
        `Import started — ${job.fileName} (${job.importType})`
      )

      // Navigate to the job detail page
      navigate(`/import/jobs/${job.id}`)
    },

    onError: (err) => {
      // Central normalizer maps 413 → "file too large", 415 → "unsupported
      // type", 429 → rate-limit, 403 → permission denied, 409 → safe conflict.
      toast.error(getErrorMessage(err))
    },
  })
}

/** Hook for downloading a CSV template. */
export function useDownloadTemplate() {
  return useMutation({
    mutationFn: (type: ImportType) => importService.downloadTemplate(type),
    onError: (err) => {
      toast.error(getErrorMessage(err))
    },
  })
}
