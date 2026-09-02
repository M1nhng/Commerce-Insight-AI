/**
 * features/ai-insights/__tests__/AiInsightsCard.test.tsx
 *
 * Rendering contract of the AI card for every mutation state. The generation
 * hook is stubbed so this file exercises only the presentation logic:
 * idle · loading · success(content) · success(empty) · unavailable · 403 · 429 ·
 * generic error — each renders a calm message and never leaks raw backend text.
 *
 * The hook itself (error normalisation, no-retry, apiClient usage) is covered by
 * useGenerateAiInsights.test.ts and by the backend integration test.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AiInsightsCard } from '../components/AiInsightsCard'
import type { AiInsightsResponse } from '../types/aiInsights.types'
import type { NormalizedApiError } from '@/lib/apiError'

const mutate = vi.fn()

type HookState = {
  mutate: typeof mutate
  data?: AiInsightsResponse
  error?: NormalizedApiError | null
  isPending: boolean
  isSuccess: boolean
  isError: boolean
}

let hookState: HookState

vi.mock('../hooks/useGenerateAiInsights', () => ({
  useGenerateAiInsights: () => hookState,
}))

const RANGE = { dateFrom: '2026-01-01T00:00:00Z', dateTo: '2026-06-30T23:59:59Z' }

function state(partial: Partial<HookState>): HookState {
  return { mutate, isPending: false, isSuccess: false, isError: false, ...partial }
}

function resp(body: Partial<AiInsightsResponse>): AiInsightsResponse {
  return {
    available: true, summary: '', insights: [], recommendations: [],
    generatedAt: '2026-09-01T00:00:00Z', provider: 'openai', model: 'gpt-4o-mini', ...body,
  }
}

beforeEach(() => {
  mutate.mockReset()
  hookState = state({})
})

describe('AiInsightsCard', () => {
  it('is idle: a Generate button, no insights, no request fired', () => {
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByRole('button', { name: /generate ai insights/i })).toBeInTheDocument()
    expect(screen.queryByText(/recommendations are AI suggestions/i)).not.toBeInTheDocument()
    expect(mutate).not.toHaveBeenCalled()
  })

  it('clicking Generate calls the mutation with the current range', async () => {
    render(<AiInsightsCard range={RANGE} />)
    await userEvent.click(screen.getByRole('button', { name: /generate ai insights/i }))
    expect(mutate).toHaveBeenCalledTimes(1)
    expect(mutate.mock.calls[0][0]).toEqual(RANGE)
  })

  it('loading: shows a busy state and disables the button', () => {
    hookState = state({ isPending: true })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText(/generating/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /generate ai insights/i })).toBeDisabled()
  })

  it('success: renders summary, insights and recommendations', () => {
    hookState = state({
      isSuccess: true,
      data: resp({
        generatedAt: new Date().toISOString(),
        summary: 'Revenue grew this period.',
        insights: [{ type: 'POSITIVE', title: 'Revenue up', description: 'Rose to 1,000.', metric: '1,000', severity: 'HIGH' }],
        recommendations: [{ title: 'Restock', description: 'Restock low items.', priority: 'MEDIUM' }],
      }),
    })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText('Revenue grew this period.')).toBeInTheDocument()
    expect(screen.getByText('Revenue up')).toBeInTheDocument()
    expect(screen.getByText('HIGH')).toBeInTheDocument()
    expect(screen.getByText('Restock')).toBeInTheDocument()
    // A fresh result reads "Generated just now"; the AI-suggestion disclaimer stays.
    expect(screen.getByText(/generated just now/i)).toBeInTheDocument()
    expect(screen.getByText(/recommendations are AI suggestions/i)).toBeInTheDocument()
  })

  it('empty: available but nothing generated', () => {
    hookState = state({ isSuccess: true, data: resp({ summary: 's', insights: [], recommendations: [] }) })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText(/no significant insights were generated/i)).toBeInTheDocument()
  })

  it('unavailable: available:false renders a calm message, no crash', () => {
    hookState = state({ isSuccess: true, data: resp({ available: false, provider: null, model: null }) })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText(/temporarily unavailable/i)).toBeInTheDocument()
  })

  it('403: shows the safe permission message from the normalised error', () => {
    hookState = state({
      isError: true,
      error: { status: 403, message: 'You do not have permission to perform this action.' },
    })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText(/do not have permission/i)).toBeInTheDocument()
  })

  it('429: shows the normalised rate-limit message', () => {
    hookState = state({
      isError: true,
      error: { status: 429, message: 'Too many requests. Please try again in 30 seconds.' },
    })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText(/too many requests/i)).toBeInTheDocument()
  })

  it('generic error: shows a safe message + retry, never raw backend detail', () => {
    hookState = state({
      isError: true,
      error: { status: 500, message: 'Something went wrong on the server. Please try again later.' },
    })
    render(<AiInsightsCard range={RANGE} />)
    expect(screen.getByText(/something went wrong on the server/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument()
    expect(screen.queryByText(/org\.springframework/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Exception/)).not.toBeInTheDocument()
  })

  it('XSS: malicious AI text is rendered as inert text, never as HTML', () => {
    const payload = '<script>alert(1)</script><img src=x onerror=alert(2)>'
    hookState = state({
      isSuccess: true,
      data: resp({
        generatedAt: new Date().toISOString(),
        summary: `Revenue ${payload} grew`,
        insights: [{ type: 'WARNING', title: payload, description: payload, metric: payload, severity: 'HIGH' }],
        recommendations: [{ title: payload, description: payload, priority: 'HIGH' }],
      }),
    })
    const { container } = render(<AiInsightsCard range={RANGE} />)

    // The payload appears as literal text content...
    expect(screen.getAllByText(new RegExp('<script>alert\\(1\\)</script>')).length).toBeGreaterThan(0)
    // ...and NOT as live DOM: no <script>, no <img> injected from the AI string.
    expect(container.querySelector('script')).toBeNull()
    expect(container.querySelector('img[onerror]')).toBeNull()
    expect(container.innerHTML).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
  })
})
