import { test, expect } from '@playwright/test'
import { loginAs, readToken } from './helpers'

// 9.3 ROLE AUTHORIZATION ─────────────────────────────────────────────────────
// Frontend guards are UI gating only; the backend stays authoritative.
test.describe('9.3 Role authorization', () => {
  test('STAFF cannot reach the ADMIN-only route', async ({ page }) => {
    await loginAs(page, 'STAFF')
    await page.goto('/admin')
    await expect(page.getByText('Access Denied')).toBeVisible()
  })

  test('MANAGER cannot reach the ADMIN-only route (no upward implication)', async ({ page }) => {
    await loginAs(page, 'MANAGER')
    await page.goto('/admin')
    await expect(page.getByText('Access Denied')).toBeVisible()
  })

  test('ADMIN can reach the ADMIN-only route', async ({ page }) => {
    await loginAs(page, 'ADMIN')
    await page.goto('/admin')
    await expect(page.getByText('Access Denied')).toHaveCount(0)
  })

  test('STAFF sees no admin nav item; MANAGER can open manager pages', async ({ page }) => {
    await loginAs(page, 'STAFF')
    await expect(page.getByRole('link', { name: /^Admin$/ })).toHaveCount(0)

    await loginAs(page, 'MANAGER')
    await page.goto('/products')
    await expect(page).toHaveURL(/\/products/)
  })
})

// 9.4 REAL BACKEND 403 ───────────────────────────────────────────────────────
test.describe('9.4 403 handling', () => {
  test('a real 403 shows PermissionDenied, keeps the session, no refresh', async ({ page }) => {
    await loginAs(page, 'STAFF')

    let refreshCalls = 0
    page.on('request', (r) => {
      if (/\/auth\/refresh$/.test(r.url())) refreshCalls += 1
    })

    // STAFF calling an ADMIN-only API (GET /users) → backend 403 ACCESS_DENIED.
    const res = await page.evaluate(async () => {
      const r = await fetch('/api/v1/users', {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      })
      return { status: r.status, body: await r.text() }
    })

    expect(res.status).toBe(403)
    expect(res.body).toContain('ACCESS_DENIED')
    // Not an auth failure: still logged in, still on the app, no refresh fired.
    expect(refreshCalls).toBe(0)
    expect(await readToken(page, 'access_token')).not.toBeNull()
    await expect(page).not.toHaveURL(/\/login/)
    // No leaked internals in the error envelope.
    expect(res.body).not.toMatch(/org\.springframework|Exception|jdbc:/)
  })
})
