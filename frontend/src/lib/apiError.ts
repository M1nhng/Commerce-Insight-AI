/**
 * lib/apiError.ts — Centralized, safe normalization of API errors.
 *
 * Every feature hook / service funnels Axios failures through here before
 * showing anything to the user. Goals:
 * - Never leak stack traces, SQL, Java exception names, JWTs, Authorization
 *   headers, MCP keys, or internal URLs into UI text.
 * - Understand the Sprint 12A backend envelope: { success, error: { code,
 *   message, details }, timestamp } plus 401/403/429 semantics.
 * - Map every status (400/401/403/404/409/413/415/429/5xx/network) to a safe
 *   sentence.
 * - Surface a backend X-Request-Id (correlation id) when one is available.
 */
import type { AxiosError } from 'axios'
import type { ApiError as BackendApiError, FieldError } from '@/types/api.types'

export interface NormalizedApiError {
  /** HTTP status, or undefined for network/timeout errors. */
  status?: number
  /** Backend error code (e.g. AUTHENTICATION_REQUIRED, RATE_LIMIT_EXCEEDED). */
  code?: string
  /** Safe, user-presentable message. Never contains internal detail. */
  message: string
  /** Correlation id echoed by the backend (X-Request-Id), if present. */
  requestId?: string
  /** Seconds to wait before retrying, from Retry-After (429 only). */
  retryAfterSeconds?: number
  /** Field-level validation errors, when the backend supplied them. */
  fieldErrors?: FieldError[]
}

/** Safe fallback copy per HTTP status. */
const STATUS_MESSAGE: Record<number, string> = {
  400: 'Invalid request.',
  401: 'Your session has expired. Please sign in again.',
  403: 'You do not have permission to perform this action.',
  404: 'The requested resource was not found.',
  409: 'This action conflicts with the current state of the data.',
  413: 'The uploaded file is too large.',
  415: 'Unsupported file type.',
  429: 'Too many requests. Please wait a moment before retrying.',
}

const SERVER_ERROR_MESSAGE =
  'Something went wrong on the server. Please try again later.'
const NETWORK_ERROR_MESSAGE = 'Unable to reach the Commerce Insight backend.'
const UNKNOWN_ERROR_MESSAGE = 'Something went wrong. Please try again.'

/**
 * Patterns that indicate a message carries internal implementation detail and
 * must NOT be shown to a user. Mirrors the guard in features/export/utils.
 */
const LEAK_PATTERN =
  /\bat [\w.$]+\(|Exception\b|Caused by:|jdbc:|SQLState|org\.springframework|com\.commerceinsight|java\.[a-z]|Bearer\s|eyJ[A-Za-z0-9_-]{5,}|Authorization:|-----BEGIN/i

/** Codes whose backend message is safe (and useful) to show verbatim. */
const TRUSTED_CODE = new Set([
  'VALIDATION_ERROR',
  'RESOURCE_CONFLICT',
  'BUSINESS_RULE_VIOLATION',
  'INSUFFICIENT_STOCK',
  'DUPLICATE_RESOURCE',
  'RESOURCE_NOT_FOUND',
  'RATE_LIMIT_EXCEEDED',
])

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null
}

/** Pull the backend envelope's `error` object out of a response body of any shape. */
function readBackendError(data: unknown): Partial<BackendApiError> | undefined {
  if (!isRecord(data)) return undefined
  const err = data.error
  if (isRecord(err)) {
    return {
      code: typeof err.code === 'string' ? err.code : undefined,
      message: typeof err.message === 'string' ? err.message : undefined,
      details: Array.isArray(err.details)
        ? (err.details as FieldError[])
        : undefined,
    }
  }
  // Some legacy endpoints put message at the top level.
  if (typeof data.message === 'string') return { message: data.message }
  return undefined
}

function safeHeader(headers: unknown, name: string): string | undefined {
  if (!isRecord(headers)) return undefined
  const raw = headers[name] ?? headers[name.toLowerCase()]
  return typeof raw === 'string' && raw.trim() ? raw.trim() : undefined
}

function parseRetryAfter(headers: unknown): number | undefined {
  const raw = safeHeader(headers, 'retry-after')
  if (!raw) return undefined
  const seconds = Number(raw)
  if (Number.isFinite(seconds) && seconds >= 0) return Math.ceil(seconds)
  // HTTP-date form — convert to a delta, clamped to a sane ceiling.
  const when = Date.parse(raw)
  if (!Number.isNaN(when)) {
    const delta = Math.ceil((when - Date.now()) / 1000)
    if (delta > 0) return Math.min(delta, 3600)
  }
  return undefined
}

function looksLikeAxiosError(err: unknown): err is AxiosError {
  return isRecord(err) && (err as { isAxiosError?: unknown }).isAxiosError === true
}

/**
 * Normalize any thrown value into a safe {@link NormalizedApiError}.
 * Accepts Axios errors, plain Errors, and unknown values.
 */
export function normalizeApiError(err: unknown): NormalizedApiError {
  // ── Non-Axios ────────────────────────────────────────────────────────────
  if (!looksLikeAxiosError(err)) {
    if (isRecord(err) && typeof err.message === 'string' && err.message) {
      const msg = LEAK_PATTERN.test(err.message)
        ? UNKNOWN_ERROR_MESSAGE
        : err.message
      return { message: msg }
    }
    return { message: UNKNOWN_ERROR_MESSAGE }
  }

  const response = err.response
  const requestId =
    safeHeader(response?.headers, 'x-request-id') ??
    safeHeader(err.config?.headers, 'x-request-id')

  // ── Network / timeout (no response) ──────────────────────────────────────
  if (!response) {
    return {
      code: err.code,
      message: NETWORK_ERROR_MESSAGE,
      requestId,
    }
  }

  const status = response.status
  const backend = readBackendError(response.data)
  const retryAfterSeconds =
    status === 429 ? parseRetryAfter(response.headers) : undefined

  // Decide the safe message.
  let message: string | undefined
  if (backend?.message && !LEAK_PATTERN.test(backend.message)) {
    const trusted =
      (backend.code && TRUSTED_CODE.has(backend.code)) ||
      status === 400 ||
      status === 409 ||
      status === 422
    if (trusted) message = backend.message
  }
  if (!message) {
    message =
      STATUS_MESSAGE[status] ??
      (status >= 500 ? SERVER_ERROR_MESSAGE : UNKNOWN_ERROR_MESSAGE)
  }

  if (status === 429 && retryAfterSeconds) {
    message = `Too many requests. Please try again in ${retryAfterSeconds} second${
      retryAfterSeconds === 1 ? '' : 's'
    }.`
  }

  return {
    status,
    code: backend?.code,
    message,
    requestId,
    retryAfterSeconds,
    fieldErrors: backend?.details,
  }
}

/** Convenience: the safe message string only. */
export function getErrorMessage(err: unknown): string {
  return normalizeApiError(err).message
}

export function isAuthError(err: unknown): boolean {
  return normalizeApiError(err).status === 401
}

export function isForbiddenError(err: unknown): boolean {
  return normalizeApiError(err).status === 403
}

export function isRateLimited(err: unknown): boolean {
  return normalizeApiError(err).status === 429
}
