import { describe, it, expect, beforeEach, vi } from 'vitest'
import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { apiClient, __resetAuthInterceptorState } from '../axios'
import { setTokens, getAccessToken, getRefreshToken } from '@/lib/authTokens'
import { queryClient } from '@/providers/QueryProvider'

// ── Adapter plumbing ──────────────────────────────────────────────────────
type Responder = (config: InternalAxiosRequestConfig) => unknown

let respond: Responder

function jsonOk(config: InternalAxiosRequestConfig, data: unknown = { ok: true }) {
  return { data, status: 200, statusText: 'OK', headers: {}, config }
}

function httpError(
  config: InternalAxiosRequestConfig,
  status: number,
  data: unknown = {},
  headers: Record<string, string> = {},
) {
  return Promise.reject(
    new AxiosError('request failed', String(status), config, {}, {
      status,
      statusText: '',
      data,
      headers,
      config,
    } as never),
  )
}

beforeEach(() => {
  vi.restoreAllMocks()
  __resetAuthInterceptorState()
  window.localStorage.clear()
  queryClient.clear()
  setTokens('access-old', 'refresh-old')
  respond = (c) => jsonOk(c)
  apiClient.defaults.adapter = (config) => Promise.resolve(respond(config)) as never
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { pathname: '/orders', search: '', assign: vi.fn(), href: '' },
  })
})

// ─────────────────────────────────────────────────────────────────────────
describe('request interceptor — Authorization header', () => {
  it('attaches Bearer <accessToken> to authenticated requests', async () => {
    let seen: string | undefined
    respond = (c) => {
      seen = c.headers?.Authorization as string
      return jsonOk(c)
    }
    await apiClient.get('/products')
    expect(seen).toBe('Bearer access-old')
  })

  it('never attaches Authorization to login / register / refresh', async () => {
    const seen: Record<string, unknown> = {}
    respond = (c) => {
      seen[c.url ?? ''] = c.headers?.Authorization
      return jsonOk(c)
    }
    await apiClient.post('/auth/login', {})
    await apiClient.post('/auth/register', {})
    expect(seen['/auth/login']).toBeUndefined()
    expect(seen['/auth/register']).toBeUndefined()
  })
})

// ─────────────────────────────────────────────────────────────────────────
describe('401 handling — single-flight refresh', () => {
  it('refreshes once then retries the original request exactly once', async () => {
    const post = vi.spyOn(axios, 'post').mockResolvedValue({
      data: { success: true, data: { accessToken: 'access-new', refreshToken: 'refresh-new' } },
    } as never)

    let calls = 0
    respond = (c) => {
      calls += 1
      return c.headers?.Authorization === 'Bearer access-new'
        ? jsonOk(c, { retried: true })
        : httpError(c, 401, { error: { code: 'AUTHENTICATION_REQUIRED' } })
    }

    const res = await apiClient.get('/orders')
    expect(res.data).toEqual({ retried: true })
    expect(post).toHaveBeenCalledTimes(1)
    expect(calls).toBe(2) // original 401 + one retry
    expect(getAccessToken()).toBe('access-new')
    expect(getRefreshToken()).toBe('refresh-new')
  })

  it('collapses concurrent 401s into ONE refresh call', async () => {
    const post = vi.spyOn(axios, 'post').mockImplementation(async () => {
      await new Promise((r) => setTimeout(r, 10))
      return {
        data: { success: true, data: { accessToken: 'access-new', refreshToken: 'refresh-new' } },
      } as never
    })

    respond = (c) =>
      c.headers?.Authorization === 'Bearer access-new'
        ? jsonOk(c)
        : httpError(c, 401, { error: { code: 'AUTHENTICATION_REQUIRED' } })

    await Promise.all([
      apiClient.get('/orders'),
      apiClient.get('/products'),
      apiClient.get('/customers'),
    ])
    expect(post).toHaveBeenCalledTimes(1)
  })

  it('does not loop: a persistent 401 after retry rejects and refreshes at most once', async () => {
    const post = vi.spyOn(axios, 'post').mockResolvedValue({
      data: { success: true, data: { accessToken: 'access-new', refreshToken: 'refresh-new' } },
    } as never)
    respond = (c) => httpError(c, 401, { error: { code: 'AUTHENTICATION_REQUIRED' } })

    await expect(apiClient.get('/orders')).rejects.toBeInstanceOf(AxiosError)
    expect(post).toHaveBeenCalledTimes(1)
  })

  it('refresh failure clears the session and redirects once', async () => {
    vi.spyOn(axios, 'post').mockRejectedValue(new AxiosError('refresh boom', '401'))
    queryClient.setQueryData(['customers'], [{ id: 'c1' }])
    respond = (c) => httpError(c, 401, { error: { code: 'AUTHENTICATION_REQUIRED' } })

    await expect(apiClient.get('/orders')).rejects.toBeTruthy()

    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
    expect(queryClient.getQueryData(['customers'])).toBeUndefined()
    expect((window.location.assign as ReturnType<typeof vi.fn>)).toHaveBeenCalled()
  })
})

// ─────────────────────────────────────────────────────────────────────────
describe('403 / 429 are not auth failures', () => {
  it('403 → no refresh, no logout, rejects', async () => {
    const post = vi.spyOn(axios, 'post')
    respond = (c) => httpError(c, 403, { error: { code: 'ACCESS_DENIED' } })

    await expect(apiClient.get('/users')).rejects.toBeInstanceOf(AxiosError)
    expect(post).not.toHaveBeenCalled()
    expect(getAccessToken()).toBe('access-old')
  })

  it('429 → no refresh, no logout, Retry-After preserved on the error', async () => {
    const post = vi.spyOn(axios, 'post')
    respond = (c) =>
      httpError(c, 429, { error: { code: 'RATE_LIMIT_EXCEEDED' } }, { 'retry-after': '12' })

    await apiClient.get('/export/orders').then(
      () => expect.unreachable('should reject'),
      (err: AxiosError) => {
        expect(err.response?.status).toBe(429)
        expect(err.response?.headers['retry-after']).toBe('12')
      },
    )
    expect(post).not.toHaveBeenCalled()
    expect(getAccessToken()).toBe('access-old')
  })
})
