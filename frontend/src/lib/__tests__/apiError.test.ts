import { describe, it, expect } from 'vitest'
import { AxiosError, AxiosHeaders } from 'axios'
import {
  normalizeApiError,
  getErrorMessage,
  isForbiddenError,
  isRateLimited,
} from '../apiError'

/** Build an AxiosError with a given response. */
function axiosErr(
  status: number | undefined,
  data?: unknown,
  headers?: Record<string, string>,
): AxiosError {
  const err = new AxiosError('Request failed')
  err.config = { headers: new AxiosHeaders() } as never
  if (status !== undefined) {
    err.response = {
      status,
      statusText: '',
      data,
      headers: new AxiosHeaders(headers ?? {}),
      config: err.config as never,
    }
  }
  return err
}

function envelope(code: string, message: string) {
  return { success: false, error: { code, message }, timestamp: '2026-08-30T00:00:00Z' }
}

describe('normalizeApiError — status mapping', () => {
  it('400 → safe generic or trusted backend message', () => {
    expect(getErrorMessage(axiosErr(400))).toBe('Invalid request.')
    expect(
      getErrorMessage(axiosErr(400, envelope('VALIDATION_ERROR', 'Email is required'))),
    ).toBe('Email is required')
  })

  it('404 → not found', () => {
    expect(getErrorMessage(axiosErr(404))).toBe('The requested resource was not found.')
  })

  it('409 → shows the (safe) backend conflict message', () => {
    const msg = getErrorMessage(
      axiosErr(409, envelope('RESOURCE_CONFLICT', 'Order could not be created: insufficient stock')),
    )
    expect(msg).toContain('insufficient stock')
  })

  it('413 → file too large', () => {
    expect(getErrorMessage(axiosErr(413))).toBe('The uploaded file is too large.')
  })

  it('415 → unsupported type', () => {
    expect(getErrorMessage(axiosErr(415))).toBe('Unsupported file type.')
  })

  it('429 → rate-limit message, no logout semantics, Retry-After parsed', () => {
    const e = axiosErr(429, envelope('RATE_LIMIT_EXCEEDED', 'slow down'), {
      'retry-after': '30',
    })
    const n = normalizeApiError(e)
    expect(n.status).toBe(429)
    expect(n.retryAfterSeconds).toBe(30)
    expect(n.message).toMatch(/try again in 30 seconds/i)
    expect(isRateLimited(e)).toBe(true)
  })

  it('500 → safe server message', () => {
    expect(getErrorMessage(axiosErr(500))).toBe(
      'Something went wrong on the server. Please try again later.',
    )
  })

  it('network error (no response) → reachability message', () => {
    expect(getErrorMessage(axiosErr(undefined))).toBe(
      'Unable to reach the Commerce Insight backend.',
    )
  })
})

describe('normalizeApiError — leak protection', () => {
  it('drops a backend message containing a stack trace / Java package', () => {
    const msg = getErrorMessage(
      axiosErr(500, envelope('INTERNAL', 'org.springframework.jdbc.BadSqlGrammarException: at com.commerceinsight.Foo(Foo.java:12)')),
    )
    expect(msg).toBe('Something went wrong on the server. Please try again later.')
  })

  it('never surfaces a bearer token embedded in an error message', () => {
    const msg = getErrorMessage(
      new Error('Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig leaked'),
    )
    expect(msg).not.toMatch(/eyJ/)
    expect(msg).not.toMatch(/Bearer/)
  })

  it('403 → permission-denied copy, flagged as forbidden', () => {
    const e = axiosErr(403, envelope('ACCESS_DENIED', 'nope'))
    expect(getErrorMessage(e)).toBe('You do not have permission to perform this action.')
    expect(isForbiddenError(e)).toBe(true)
  })
})

describe('normalizeApiError — correlation id', () => {
  it('captures X-Request-Id from the response headers', () => {
    const n = normalizeApiError(axiosErr(500, undefined, { 'x-request-id': 'abc-123' }))
    expect(n.requestId).toBe('abc-123')
  })
})
