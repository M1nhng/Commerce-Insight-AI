/**
 * features/import/hooks/useImportJobs.ts
 * TanStack Query hooks for reading import job data.
 */
import { useQuery } from '@tanstack/react-query'
import { importService } from '../services/importService'
import type { ImportJobFilterParams, ImportErrorFilterParams } from '../types/import.types'

// ── Query keys ────────────────────────────────────────────────────────────

export const IMPORT_KEYS = {
  all:    ['imports'] as const,
  jobs:   (params: ImportJobFilterParams) => ['imports', 'jobs', params] as const,
  job:    (id: string) => ['imports', 'job', id] as const,
  errors: (id: string, params: ImportErrorFilterParams) =>
    ['imports', 'errors', id, params] as const,
}

// ── Query hooks ───────────────────────────────────────────────────────────

/** Paginated list of import jobs, with optional filters. */
export function useImportJobs(params: ImportJobFilterParams = {}) {
  return useQuery({
    queryKey: IMPORT_KEYS.jobs(params),
    queryFn:  () => importService.getJobs(params),
    select:   (data) => data.data,
    staleTime: 15_000,
  })
}

/** Single import job detail by ID. */
export function useImportJob(id: string | null) {
  return useQuery({
    queryKey: IMPORT_KEYS.job(id ?? ''),
    queryFn:  () => importService.getJob(id!),
    select:   (data) => data.data,
    enabled:  !!id,
    staleTime: 10_000,
    // Refetch while job is still processing
    refetchInterval: (query) => {
      const status = query.state.data?.data?.status
      if (status === 'VALIDATING' || status === 'IMPORTING' || status === 'UPLOADED') {
        return 3_000 // poll every 3s while active
      }
      return false
    },
  })
}

/** Paginated error list for a specific import job. */
export function useImportJobErrors(
  jobId: string | null,
  params: ImportErrorFilterParams = {}
) {
  return useQuery({
    queryKey: IMPORT_KEYS.errors(jobId ?? '', params),
    queryFn:  () => importService.getJobErrors(jobId!, params),
    select:   (data) => data.data,
    enabled:  !!jobId,
    staleTime: 30_000,
  })
}
