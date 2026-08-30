import { test, expect } from '@playwright/test'
import { loginAs, corruptAccessToken, readToken, countRequests } from './helpers'

// 9.5 401 → SINGLE REFRESH → RETRY ONCE (hits the real backend) ───────────────
test.describe('9.5 401 → refresh', () => {
  test('expired access token: one real /auth/refresh, original retried, no logout', async ({
    page,
  }) => {
    await loginAs(page, 'STAFF')
    const originalRefresh = await readToken(page, 'refresh_token')
    await corruptAccessToken(page)

    const refreshCalls = await countRequests(page, /\/auth\/refresh$/, async () => {
      // App-driven protected call: the products list query → 401 → the Axios
      // interceptor performs ONE real /auth/refresh, then retries the query.
      await page.goto('/products')
      await expect(page).toHaveURL(/\/products/)
      await page.waitForLoadState('networkidle')
    })

    expect(refreshCalls).toBe(1)
    expect(await readToken(page, 'access_token')).not.toBeNull()
    expect(await readToken(page, 'refresh_token')).not.toBe(originalRefresh) // rotated
    await expect(page).not.toHaveURL(/\/login/)
    // The retried request succeeded — the products view rendered its table/empty state.
    await expect(page.getByRole('heading', { name: /^Products$/ })).toBeVisible()
  })
})

// 9.6 CONCURRENT 401 → SINGLE-FLIGHT ─────────────────────────────────────────
test.describe('9.6 Concurrent 401', () => {
  test('a reload that fires many parallel 401s collapses into exactly ONE refresh', async ({
    page,
  }) => {
    await loginAs(page, 'ADMIN')
    // Inventory mounts several parallel useQuery hooks (list + low-stock + …).
    await page.goto('/inventory')
    await page.waitForLoadState('networkidle')

    await corruptAccessToken(page)

    const refreshCalls = await countRequests(page, /\/auth\/refresh$/, async () => {
      // On reload: AuthProvider.initialize() (/auth/me) + every page query fire
      // together through the shared apiClient, all getting 401 at once.
      await page.reload()
      await page.waitForLoadState('networkidle')
    })

    expect(refreshCalls).toBe(1) // single-flight
    await expect(page).toHaveURL(/\/inventory/)
    await expect(page).not.toHaveURL(/\/login/)
  })
})
