/**
 * features/ai-insights/components/AiInsightsCard.tsx
 *
 * "AI Business Insights" — one card on the existing analytics dashboard.
 *
 * Generation is user-triggered (a mutation), never automatic. States handled:
 *   idle · loading · success(available) · success(empty) · unavailable ·
 *   permission-denied (403) · rate-limited (429) · generic error.
 *
 * All AI text is rendered as plain JSX text — no raw-HTML injection sink is
 * used anywhere in this file. Errors come from the shared lib/apiError
 * normaliser, so no stack trace, SQL, provider name, or token can reach the DOM.
 */
import {
  Sparkles, TrendingUp, TrendingDown, AlertTriangle, Lightbulb, Activity,
  ShieldAlert, Clock, RefreshCw,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useGenerateAiInsights } from '../hooks/useGenerateAiInsights'
import type { AnalyticsDateRange } from '@/features/analytics'
import type { AiInsight, AiInsightType, AiInsightSeverity } from '../types/aiInsights.types'

interface Props {
  range: AnalyticsDateRange
}

const TYPE_META: Record<AiInsightType, { icon: React.ElementType; color: string }> = {
  POSITIVE:    { icon: TrendingUp,   color: '#34d399' },
  NEGATIVE:    { icon: TrendingDown, color: '#f87171' },
  WARNING:     { icon: AlertTriangle, color: '#fbbf24' },
  OPPORTUNITY: { icon: Lightbulb,    color: '#818cf8' },
  TREND:       { icon: Activity,     color: '#22d3ee' },
}

const SEVERITY_COLOR: Record<AiInsightSeverity, string> = {
  LOW: '#64748b', MEDIUM: '#f59e0b', HIGH: '#ef4444',
}
const PRIORITY_COLOR = SEVERITY_COLOR

function Pill({ label, color }: { label: string; color: string }) {
  return (
    <span
      className="rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide"
      style={{ background: `${color}22`, color }}
    >
      {label}
    </span>
  )
}

function InsightRow({ insight }: { insight: AiInsight }) {
  const meta = TYPE_META[insight.type] ?? TYPE_META.TREND
  const Icon = meta.icon
  return (
    <div className="flex gap-3 py-3">
      <div
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg"
        style={{ background: `${meta.color}18` }}
      >
        <Icon className="h-4 w-4" style={{ color: meta.color }} />
      </div>
      <div className="min-w-0 flex-1">
        <div className="mb-0.5 flex flex-wrap items-center gap-2">
          <p className="text-sm font-semibold" style={{ color: 'var(--text-primary)' }}>
            {insight.title || 'Insight'}
          </p>
          <Pill label={insight.severity} color={SEVERITY_COLOR[insight.severity] ?? SEVERITY_COLOR.LOW} />
        </div>
        {insight.description && (
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>{insight.description}</p>
        )}
        {insight.metric && (
          <p className="mt-0.5 text-xs" style={{ color: 'var(--text-muted)' }}>{insight.metric}</p>
        )}
      </div>
    </div>
  )
}

export function AiInsightsCard({ range }: Props) {
  const mutation = useGenerateAiInsights()
  const { data, error, isPending, isSuccess, isError } = mutation

  // Fire-and-forget; the error is read from mutation.error for the UI. The
  // explicit no-op onError keeps the rejection fully handled.
  const generate = () => {
    mutation.mutate(range, { onError: () => undefined })
  }

  const isForbidden = isError && error?.status === 403
  const isRateLimited = isError && error?.status === 429
  const hasContent =
    isSuccess && data?.available && (data.insights.length > 0 || data.recommendations.length > 0)
  const isEmpty =
    isSuccess && data?.available && data.insights.length === 0 && data.recommendations.length === 0
  const isUnavailable = isSuccess && data && !data.available

  return (
    <div
      className="rounded-xl p-5"
      style={{ background: 'var(--bg-surface)', border: '1px solid var(--border-default)' }}
    >
      {/* Header */}
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5" style={{ color: 'var(--accent-400)' }} />
          <h2 className="text-base font-semibold" style={{ color: 'var(--text-primary)' }}>
            AI Business Insights
          </h2>
        </div>
        <Button
          size="sm"
          variant="outline"
          onClick={generate}
          disabled={isPending}
          aria-label={isSuccess || isError ? 'Refresh AI insights' : 'Generate AI insights'}
        >
          <RefreshCw className={`h-3.5 w-3.5 ${isPending ? 'animate-spin' : ''}`} />
          {isPending ? 'Generating…' : isSuccess || isError ? 'Refresh insights' : 'Generate insights'}
        </Button>
      </div>

      {/* Idle */}
      {!isPending && !isSuccess && !isError && (
        <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
          Generate a short, plain-language read of this period&apos;s performance — key
          insights and recommendations, based only on the analytics above.
        </p>
      )}

      {/* Loading */}
      {isPending && (
        <div className="space-y-3" aria-busy="true">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-4 animate-pulse rounded" style={{ background: 'var(--bg-elevated)' }} />
          ))}
          <div className="h-4 w-2/3 animate-pulse rounded" style={{ background: 'var(--bg-elevated)' }} />
        </div>
      )}

      {/* Permission denied (403) */}
      {isForbidden && (
        <div className="flex flex-col items-center gap-2 py-6 text-center">
          <ShieldAlert className="h-7 w-7" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            {error?.message ?? 'You do not have permission to generate AI insights.'}
          </p>
        </div>
      )}

      {/* Rate limited (429) */}
      {isRateLimited && (
        <div className="flex flex-col items-center gap-2 py-6 text-center">
          <Clock className="h-7 w-7" style={{ color: 'var(--warning, #f59e0b)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>{error?.message}</p>
        </div>
      )}

      {/* Generic error */}
      {isError && !isForbidden && !isRateLimited && (
        <div className="flex flex-col items-center gap-3 py-6 text-center">
          <AlertTriangle className="h-7 w-7" style={{ color: 'var(--error)' }} />
          <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>
            {error?.message ?? 'Unable to generate AI insights right now.'}
          </p>
          <Button size="sm" variant="outline" onClick={generate}>Try again</Button>
        </div>
      )}

      {/* Unavailable (feature off / provider down) — dashboard is unaffected */}
      {isUnavailable && (
        <p className="py-4 text-sm" style={{ color: 'var(--text-secondary)' }}>
          AI insights are temporarily unavailable. The rest of the dashboard is unaffected.
        </p>
      )}

      {/* Empty */}
      {isEmpty && (
        <p className="py-4 text-sm" style={{ color: 'var(--text-secondary)' }}>
          No significant insights were generated for this period.
        </p>
      )}

      {/* Success with content */}
      {hasContent && data && (
        <div className="space-y-4">
          <p className="text-sm" style={{ color: 'var(--text-primary)' }}>{data.summary}</p>

          {data.insights.length > 0 && (
            <div>
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--text-muted)' }}>
                Key insights
              </p>
              <div className="divide-y" style={{ borderColor: 'var(--border-subtle)' }}>
                {data.insights.map((ins, i) => (
                  <InsightRow key={`${ins.title}-${i}`} insight={ins} />
                ))}
              </div>
            </div>
          )}

          {data.recommendations.length > 0 && (
            <div>
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--text-muted)' }}>
                Recommendations
              </p>
              <ul className="space-y-2">
                {data.recommendations.map((rec, i) => (
                  <li key={`${rec.title}-${i}`} className="flex gap-2">
                    <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full" style={{ background: 'var(--accent-400)' }} />
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>{rec.title}</p>
                        <Pill label={rec.priority} color={PRIORITY_COLOR[rec.priority] ?? PRIORITY_COLOR.MEDIUM} />
                      </div>
                      {rec.description && (
                        <p className="text-sm" style={{ color: 'var(--text-secondary)' }}>{rec.description}</p>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
            Generated {new Date(data.generatedAt).toLocaleString()} · recommendations are AI suggestions, not observed facts
          </p>
        </div>
      )}
    </div>
  )
}
