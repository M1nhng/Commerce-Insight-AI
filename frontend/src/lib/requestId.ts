/**
 * lib/requestId.ts — Last-seen backend correlation id (X-Request-Id).
 *
 * The Axios response interceptor records the most recent `X-Request-Id` here so
 * error surfaces can show a "Reference ID" for support without exposing any
 * other request metadata. This is deliberately a single value, not a log — we
 * never persist a history and never pair it with tokens.
 */
let lastRequestId: string | undefined

/** Basic shape guard so a malformed header can't poison the UI. */
const SAFE_ID = /^[A-Za-z0-9._-]{1,64}$/

export function setLastRequestId(id: string | undefined): void {
  if (id && SAFE_ID.test(id)) lastRequestId = id
}

export function getLastRequestId(): string | undefined {
  return lastRequestId
}
