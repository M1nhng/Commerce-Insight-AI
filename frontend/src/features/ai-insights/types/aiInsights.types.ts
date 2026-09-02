/**
 * features/ai-insights/types/aiInsights.types.ts
 *
 * Mirrors the Spring Boot AI-insights contract exactly:
 *   - AiInsightsRequest.java
 *   - AiInsightsResponse.java / AiInsight.java / AiRecommendation.java
 *
 * The response is provider-agnostic — no raw OpenAI/Claude/Gemini shape ever
 * reaches the browser. Strings are rendered as plain text (no HTML sink).
 */

export type AiInsightType =
  | 'POSITIVE'
  | 'NEGATIVE'
  | 'WARNING'
  | 'OPPORTUNITY'
  | 'TREND'

export type AiInsightSeverity = 'LOW' | 'MEDIUM' | 'HIGH'

export type AiRecommendationPriority = 'LOW' | 'MEDIUM' | 'HIGH'

/** Mirrors AiInsight.java */
export interface AiInsight {
  type: AiInsightType
  title: string
  description: string
  metric: string
  severity: AiInsightSeverity
}

/** Mirrors AiRecommendation.java */
export interface AiRecommendation {
  title: string
  description: string
  priority: AiRecommendationPriority
}

/**
 * Mirrors AiInsightsResponse.java.
 * `available === false` is the safe degraded form (feature off / provider down);
 * arrays are empty and provider/model are null in that case.
 */
export interface AiInsightsResponse {
  available: boolean
  summary: string
  insights: AiInsight[]
  recommendations: AiRecommendation[]
  generatedAt: string
  provider: string | null
  model: string | null
}

/** Mirrors AiInsightsRequest.java — both bounds required. */
export interface AiInsightsRequest {
  dateFrom: string
  dateTo: string
}
