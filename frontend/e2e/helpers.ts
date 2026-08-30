import { expect, type Page } from '@playwright/test'

/**
 * Deterministic RBAC test users. Seeded by
 * backend/src/main/resources/db/e2e/R__seed_e2e_users.sql (e2e profile only).
 * Overridable via env for CI secret management.
 */
export const USERS = {
  STAFF: {
    email: process.env.E2E_STAFF_EMAIL ?? 'e2e-staff@commerceinsight.test',
    password: process.env.E2E_STAFF_PASSWORD ?? 'E2eStaff!234',
  },
  MANAGER: {
    email: process.env.E2E_MANAGER_EMAIL ?? 'e2e-manager@commerceinsight.test',
    password: process.env.E2E_MANAGER_PASSWORD ?? 'E2eManager!234',
  },
  ADMIN: {
    email: process.env.E2E_ADMIN_EMAIL ?? 'e2e-admin@commerceinsight.test',
    password: process.env.E2E_ADMIN_PASSWORD ?? 'E2eAdmin!234',
  },
} as const

export type RoleName = keyof typeof USERS

/** A syntactically valid JWT the backend will always reject (bad signature). */
export const BOGUS_JWT =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.' +
  'eyJzdWIiOiJlMmUiLCJ0eXAiOiJhY2Nlc3MiLCJpc3MiOiJjb21tZXJjZS1pbnNpZ2h0LWFpIiwiZXhwIjo0ODY1NzA1NjAwfQ.' +
  'not-a-real-signature-000000000000000000000000'

/** Log in through the real UI and land on an authenticated page. */
export async function loginAs(page: Page, role: RoleName): Promise<void> {
  const { email, password } = USERS[role]
  await page.goto('/login')
  await page.locator('#email').fill(email)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: /sign in/i }).click()
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 })
}

export async function readToken(page: Page, key: 'access_token' | 'refresh_token') {
  return page.evaluate((k) => window.localStorage.getItem(k), key)
}

/** Replace only the access token so the NEXT protected call returns a real 401. */
export async function corruptAccessToken(page: Page): Promise<void> {
  await page.evaluate((bogus) => {
    window.localStorage.setItem('access_token', bogus)
  }, BOGUS_JWT)
}

/** Invalidate both tokens so a refresh attempt also fails. */
export async function corruptAllTokens(page: Page): Promise<void> {
  await page.evaluate((bogus) => {
    window.localStorage.setItem('access_token', bogus)
    window.localStorage.setItem('refresh_token', 'definitely-not-a-valid-refresh-token')
  }, BOGUS_JWT)
}

/** Count network requests whose URL matches, for the duration of `fn`. */
export async function countRequests(
  page: Page,
  matcher: RegExp,
  fn: () => Promise<void>,
): Promise<number> {
  let n = 0
  const listener = (req: { url(): string }) => {
    if (matcher.test(req.url())) n += 1
  }
  page.on('request', listener)
  try {
    await fn()
  } finally {
    page.off('request', listener)
  }
  return n
}
