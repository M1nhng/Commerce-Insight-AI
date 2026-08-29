/**
 * features/export/utils/dateRange.ts
 *
 * Converts the `<input type="date">` values used by the export filters into the
 * ISO-8601 UTC instants the Sprint 11A backend expects
 * (java.time.Instant + @DateTimeFormat(iso = DATE_TIME)).
 *
 * Mirrors the approach already used by
 * features/analytics/components/AnalyticsDateFilter.tsx.
 */
import { startOfDay, endOfDay } from 'date-fns'

/** "2026-08-01" → "2026-08-01T00:00:00.000Z" (start of that local day, as UTC). */
export function toStartInstant(dateInput: string | undefined | null): string | undefined {
  if (!dateInput) return undefined
  const d = new Date(`${dateInput}T00:00:00`)
  if (Number.isNaN(d.getTime())) return undefined
  return startOfDay(d).toISOString()
}

/** "2026-08-31" → "2026-08-31T23:59:59.999Z" (end of that local day, as UTC). */
export function toEndInstant(dateInput: string | undefined | null): string | undefined {
  if (!dateInput) return undefined
  const d = new Date(`${dateInput}T00:00:00`)
  if (Number.isNaN(d.getTime())) return undefined
  return endOfDay(d).toISOString()
}

/**
 * True when the range is usable: either bound may be empty, but if BOTH are set
 * then `from` must not be after `to`.
 */
export function isValidRange(
  fromInput: string | undefined | null,
  toInput: string | undefined | null,
): boolean {
  if (!fromInput || !toInput) return true
  return new Date(fromInput).getTime() <= new Date(toInput).getTime()
}

/** Human display for a raw yyyy-mm-dd input value (empty string when unset). */
export function displayDate(dateInput: string | undefined | null): string {
  if (!dateInput) return ''
  const [y, m, d] = dateInput.split('-')
  return y && m && d ? `${d}/${m}/${y}` : dateInput
}
