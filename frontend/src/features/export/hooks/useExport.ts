/**
 * features/export/hooks/useExport.ts
 *
 * Mutation hook for running a report export. Export is a one-shot side effect
 * (download a file), so it is modelled with useMutation — never useQuery.
 *
 * The mutation's `isPending` drives the loading state and disables the button,
 * which also prevents duplicate concurrent exports.
 */
import { useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { exportService } from '../services/exportService'
import type { ExportDownloadResult, ExportRequest } from '../types/export.types'

export function useExportReport() {
  return useMutation<ExportDownloadResult, Error, ExportRequest>({
    mutationFn: (request) => exportService.run(request),

    onSuccess: (result) => {
      toast.success(`Report exported successfully — ${result.filename}`)
    },

    onError: (err) => {
      toast.error(
        err.message || 'Unable to generate the report right now. Please try again.',
      )
    },
  })
}
