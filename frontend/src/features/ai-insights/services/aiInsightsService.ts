/**
 * features/ai-insights/services/aiInsightsService.ts
 *
 * Single call: POST /api/v1/analytics/ai-insights.
 *
 * Rules (same as analyticsService):
 * - uses the shared apiClient — no second Axios instance, no direct fetch;
 * - no LLM provider is ever contacted from the browser;
 * - returns the ApiResponse<T> body; errors propagate to the caller's hook
 *   which normalises them via lib/apiError.
 */
import { apiClient } from '@/services/axios'
import type { ApiResponse } from '@/types/api.types'
import type { AiInsightsRequest, AiInsightsResponse } from '../types/aiInsights.types'
import type { AnalyticsDateRange } from '@/features/analytics'

export const aiInsightsService = {
  /**
   * POST /api/v1/analytics/ai-insights
   * Both bounds are required by the backend; a missing bound → 400.
   */
  generate(range: AnalyticsDateRange) {
    const body: AiInsightsRequest = {
      dateFrom: range.dateFrom ?? '',
      dateTo: range.dateTo ?? '',
    }
    return apiClient
      .post<ApiResponse<AiInsightsResponse>>('/analytics/ai-insights', body)
      .then((r) => r.data)
  },
}
