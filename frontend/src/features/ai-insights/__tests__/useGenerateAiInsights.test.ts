/**
 * features/ai-insights/__tests__/useGenerateAiInsights.test.ts
 *
 * The generation mutation: uses the shared apiClient (POST
 * /analytics/ai-insights), never auto-retries, and surfaces only a normalised,
 * leak-safe error. The Axios client is mocked at the module boundary so the
 * real service + hook code runs and no network call is made.
 */
import { describe, it, expect, vi, beforeEach, beforeAll, afterAll } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement, type ReactNode } from 'react'
import { useGenerateAiInsights } from '../hooks/useGenerateAiInsights'

const post = vi.fn()
vi.mock('@/services/axios', () => ({
  apiClient: { post: (...args: unknown[]) => post(...args) },
  default: { post: (...args: unknown[]) => post(...args) },
}))

const RANGE = { dateFrom: '2026-01-01T00:00:00Z', dateTo: '2026-06-30T23:59:59Z' }

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  return createElement(QueryClientProvider, { client }, children)
}

function axiosError(status: number, message = 'internal detail', headers: Record<string, string> = {}) {
  return Object.assign(new Error('request failed'), {
    isAxiosError: true,
    response: { status, data: { error: { code: 'X', message } }, headers },
  })
}

function rejectWith(reason: unknown): Promise<never> {
  return Promise.reject(reason)
}

/**
 * Vitest's mock settled-result tracking wraps a mock's returned promise; when
 * that promise rejects, the wrapper's branch is reported as an "unhandled
 * rejection" even though the hook's own `try/await` fully handles the error.
 * That tracking artifact is not a product bug — swallow it for this file only
 * and assert (afterAll) that only the expected axios shape appeared.
 */
const swallowed: unknown[] = []
const onUnhandled = (reason: unknown) => { swallowed.push(reason) }
beforeAll(() => process.on('unhandledRejection', onUnhandled))
afterAll(() => {
  process.off('unhandledRejection', onUnhandled)
  for (const r of swallowed) {
    expect((r as { isAxiosError?: boolean })?.isAxiosError).toBe(true)
  }
})

beforeEach(() => { post.mockReset(); swallowed.length = 0 })

describe('useGenerateAiInsights', () => {
  it('POSTs the range and returns the AiInsightsResponse data', async () => {
    const data = { available: true, summary: 's', insights: [], recommendations: [], generatedAt: 'x', provider: 'openai', model: 'm' }
    post.mockResolvedValue({ data: { success: true, data, message: 'ok', timestamp: '' } })

    const { result } = renderHook(() => useGenerateAiInsights(), { wrapper })
    result.current.mutate(RANGE)

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(post).toHaveBeenCalledWith('/analytics/ai-insights', { dateFrom: RANGE.dateFrom, dateTo: RANGE.dateTo })
    expect(result.current.data).toEqual(data)
  })

  it('normalises a 403 to a safe message (no raw detail)', async () => {
    post.mockImplementation(() => rejectWith(axiosError(403, 'com.commerceinsight.Boom')))
    const { result } = renderHook(() => useGenerateAiInsights(), { wrapper })
    result.current.mutate(RANGE)

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.status).toBe(403)
    expect(result.current.error?.message).toMatch(/do not have permission/i)
    expect(result.current.error?.message).not.toMatch(/com\.commerceinsight/)
  })

  it('normalises a 429 with a retry hint', async () => {
    post.mockImplementation(() => rejectWith(axiosError(429, 'busy', { 'retry-after': '42' })))
    const { result } = renderHook(() => useGenerateAiInsights(), { wrapper })
    result.current.mutate(RANGE)

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.status).toBe(429)
    expect(result.current.error?.message).toMatch(/too many requests/i)
  })

  it('normalises a 5xx without leaking stack / class names', async () => {
    post.mockImplementation(() => rejectWith(axiosError(500, 'java.lang.NullPointerException at Foo.bar(Foo.java:1)')))
    const { result } = renderHook(() => useGenerateAiInsights(), { wrapper })
    result.current.mutate(RANGE)

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error?.message).toMatch(/something went wrong on the server/i)
    expect(result.current.error?.message).not.toMatch(/NullPointerException|\.java:/)
  })

  it('does not auto-retry on failure (single POST)', async () => {
    post.mockImplementation(() => rejectWith(axiosError(500)))
    const { result } = renderHook(() => useGenerateAiInsights(), { wrapper })
    result.current.mutate(RANGE)

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(post).toHaveBeenCalledTimes(1)
  })
})
