/**
 * features/import/types/import.types.ts
 *
 * TypeScript types mirroring the Spring Boot DTOs exactly.
 * Keep in sync with backend com.commerceinsight.dataimport.dto.*
 */

// ── Enums ─────────────────────────────────────────────────────────────────

export type ImportType = 'PRODUCT' | 'CUSTOMER' | 'ORDER'

export type ImportFileType = 'CSV' | 'XLSX'

export type ImportJobStatus =
  | 'UPLOADED'
  | 'VALIDATING'
  | 'IMPORTING'
  | 'COMPLETED'
  | 'PARTIAL_SUCCESS'
  | 'FAILED'

// ── Response DTOs (match backend exactly) ─────────────────────────────────

/** Full job detail — returned from POST /import/* and GET /import/jobs/{id} */
export interface ImportJobResponse {
  id: string
  fileName: string
  fileType: ImportFileType
  importType: ImportType
  status: ImportJobStatus
  totalRows: number
  successfulRows: number
  failedRows: number
  startedAt: string | null
  completedAt: string | null
  createdAt: string
  createdByEmail: string | null
}

/** Condensed job summary — returned from GET /import/jobs list */
export interface ImportJobSummaryResponse {
  id: string
  fileName: string
  fileType: ImportFileType
  importType: ImportType
  status: ImportJobStatus
  totalRows: number
  successfulRows: number
  failedRows: number
  createdAt: string
}

/** Single row error — returned from GET /import/jobs/{id}/errors */
export interface ImportErrorResponse {
  id: string
  rowNumber: number
  fieldName: string | null
  rawValue: string | null
  errorCode: string
  errorMessage: string
}

// ── Request / filter params ────────────────────────────────────────────────

export interface ImportJobFilterParams {
  importType?: ImportType
  status?: ImportJobStatus
  page?: number
  size?: number
}

export interface ImportErrorFilterParams {
  fieldName?: string
  errorCode?: string
  page?: number
  size?: number
}
