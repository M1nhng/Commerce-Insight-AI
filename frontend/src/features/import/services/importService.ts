/**
 * features/import/services/importService.ts
 * API calls for the data import domain.
 *
 * Endpoints consumed:
 *   POST /api/v1/import/products   — multipart upload
 *   POST /api/v1/import/customers  — multipart upload
 *   POST /api/v1/import/orders     — multipart upload
 *   GET  /api/v1/import/jobs       — paginated job list
 *   GET  /api/v1/import/jobs/{id}  — job detail
 *   GET  /api/v1/import/jobs/{id}/errors — paginated errors
 *   GET  /api/v1/import/templates/{type} — CSV template download (blob)
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse, PageResponse } from '@/types/api.types'
import type {
  ImportType,
  ImportJobResponse,
  ImportJobSummaryResponse,
  ImportErrorResponse,
  ImportJobFilterParams,
  ImportErrorFilterParams,
} from '../types/import.types'

const BASE = '/import'

/** Maps ImportType to the correct endpoint suffix. */
const endpointFor = (type: ImportType): string => {
  switch (type) {
    case 'PRODUCT':  return 'products'
    case 'CUSTOMER': return 'customers'
    case 'ORDER':    return 'orders'
  }
}

export const importService = {
  /**
   * POST /api/v1/import/{type}s
   * Uploads a CSV or XLSX file for the given import type.
   * Returns a full ImportJobResponse (HTTP 201).
   */
  upload(type: ImportType, file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<ApiResponse<ImportJobResponse>>(
        `${BASE}/${endpointFor(type)}`,
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
          // 5 minutes — large files may take time
          timeout: 5 * 60 * 1000,
        }
      )
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/import/jobs
   * Returns a paginated list of ImportJobSummaryResponse.
   */
  getJobs(params: ImportJobFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<ImportJobSummaryResponse>>>(`${BASE}/jobs`, { params })
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/import/jobs/{id}
   * Returns full job detail including counters.
   */
  getJob(id: string) {
    return apiClient
      .get<ApiResponse<ImportJobResponse>>(`${BASE}/jobs/${id}`)
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/import/jobs/{id}/errors
   * Returns paginated per-row errors for a given job.
   */
  getJobErrors(id: string, params: ImportErrorFilterParams = {}) {
    return apiClient
      .get<ApiResponse<PageResponse<ImportErrorResponse>>>(
        `${BASE}/jobs/${id}/errors`,
        { params }
      )
      .then((r) => r.data)
  },

  /**
   * GET /api/v1/import/templates/{type}
   * Downloads a CSV template file. Triggers browser file download.
   */
  async downloadTemplate(type: ImportType): Promise<void> {
    const response = await apiClient.get<Blob>(
      `${BASE}/templates/${type}`,
      { responseType: 'blob' }
    )
    // Extract filename from Content-Disposition header if available
    const disposition = response.headers['content-disposition'] as string | undefined
    const match = disposition?.match(/filename="?([^"]+)"?/)
    const filename = match?.[1] ?? `${type.toLowerCase()}_import_template.csv`

    // Trigger browser download
    const url = URL.createObjectURL(response.data)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  },
}
