/**
 * features/ai-insights — Public API
 *
 * AI-assisted narrative over the existing analytics data. One card on the
 * analytics dashboard; generation is an explicit user action.
 */
export { AiInsightsCard } from './components/AiInsightsCard'
export { useGenerateAiInsights } from './hooks/useGenerateAiInsights'
export { aiInsightsService } from './services/aiInsightsService'

export type {
  AiInsightType,
  AiInsightSeverity,
  AiRecommendationPriority,
  AiInsight,
  AiRecommendation,
  AiInsightsResponse,
  AiInsightsRequest,
} from './types/aiInsights.types'
