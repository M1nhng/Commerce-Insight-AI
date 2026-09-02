/**
 * features/ai-insights/hooks/useGenerateAiInsights.ts
 *
 * AI insight generation is an explicit user action (an expensive LLM call), so
 * it is a TanStack **mutation**, not a query — nothing runs on render, on a
 * filter keystroke, or on a polling cycle.
 *
 * - uses the shared apiClient (via aiInsightsService);
 * - never auto-retries (global mutation retry is 0; set here too for clarity);
 * - errors are normalised through the existing lib/apiError — 401 is handled by
 *   the Axios refresh interceptor, 403 / 429 never log the user out.
 */
import { useMutation } from '@tanstack/react-query'
import { aiInsightsService } from '../services/aiInsightsService'
import { normalizeApiError, type NormalizedApiError } from '@/lib/apiError'
import type { AnalyticsDateRange } from '@/features/analytics'
import type { AiInsightsResponse } from '../types/aiInsights.types'

export function useGenerateAiInsights() {
  return useMutation<AiInsightsResponse, NormalizedApiError, AnalyticsDateRange>({
    mutationKey: ['ai-insights', 'generate'],
    retry: 0,
    mutationFn: async (range) => {
      try {
        const res = await aiInsightsService.generate(range)
        if (!res.data) {
          throw { message: 'AI insights are temporarily unavailable.' } as NormalizedApiError
        }
        return res.data
      } catch (err) {
        // Reject with the normalised, leak-safe error so components never see raw Axios.
        throw normalizeApiError(err)
      }
    },
  })
}
