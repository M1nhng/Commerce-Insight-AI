import { test, expect } from '@playwright/test'
import { loginAs, readToken, corruptAllTokens, USERS } from './helpers'

// 9.1 LOGIN ──────────────────────────────────────────────────────────────────
test.describe('9.1 Login', () => {
  for (const role of ['STAFF', 'MANAGER', 'ADMIN'] as const) {
    test(`valid ${role} login reaches the protected UI`, async ({ page }) => {
      await loginAs(page, role)
      await expect(page).toHaveURL(/\/dashboard/)
      // No token text rendered anywhere on the page.
      const body = await page.locator('body').innerText()
      expect(body).not.toMatch(/eyJ[A-Za-z0-9_-]{10,}/)
      expect(body).not.toContain(USERS[role].password)
    })
  }

  test('invalid password does not authenticate', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#email').fill(USERS.STAFF.email)
    await page.locator('#password').fill('WrongPassword!999')
    await page.getByRole('button', { name: /sign in/i }).click()
    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page).toHaveURL(/\/login/)
    expect(await readToken(page, 'access_token')).toBeNull()
  })

  test('password field is cleared after submit', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#email').fill(USERS.STAFF.email)
    await page.locator('#password').fill('WrongPassword!999')
    await page.getByRole('button', { name: /sign in/i }).click()
    await expect(page.getByRole('alert')).toBeVisible()
    await expect(page.locator('#password')).toHaveValue('')
  })

  test('duplicate submit triggers at most one auth request', async ({ page }) => {
    await page.goto('/login')
    await page.locator('#email').fill(USERS.STAFF.email)
    await page.locator('#password').fill(USERS.STAFF.password)
    let loginCalls = 0
    page.on('request', (r) => {
      if (/\/auth\/login$/.test(r.url()) && r.method() === 'POST') loginCalls += 1
    })
    const btn = page.getByRole('button', { name: /sign in/i })
    await Promise.all([btn.click(), btn.click().catch(() => {})])
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 })
    expect(loginCalls).toBe(1)
  })
})

// 9.2 PROTECTED ROUTES ───────────────────────────────────────────────────────
test.describe('9.2 Protected routes', () => {
  test('unauthenticated access redirects to /login', async ({ page }) => {
    await page.goto('/orders')
    await expect(page).toHaveURL(/\/login/)
  })

  test('no protected content flashes before auth resolves', async ({ page }) => {
    await page.goto('/orders')
    // The Orders heading must never appear on the way to /login.
    await expect(page.getByRole('heading', { name: /^Orders$/ })).toHaveCount(0)
    await expect(page).toHaveURL(/\/login/)
  })

  test('authenticated user can load a protected route', async ({ page }) => {
    await loginAs(page, 'STAFF')
    await page.goto('/orders')
    await expect(page).toHaveURL(/\/orders/)
  })
})

// 9.7 REFRESH FAILURE ────────────────────────────────────────────────────────
test.describe('9.7 Refresh failure', () => {
  test('bad tokens → cleared session, one redirect to /login?session=expired', async ({ page }) => {
    await loginAs(page, 'STAFF')
    await corruptAllTokens(page)

    let redirects = 0
    page.on('framenavigated', (f) => {
      if (f === page.mainFrame() && /\/login\?session=expired/.test(f.url())) redirects += 1
    })

    await page.goto('/orders') // triggers a protected API call → 401 → refresh → fail
    await expect(page).toHaveURL(/\/login\?session=expired&redirect=/)
    expect(await readToken(page, 'access_token')).toBeNull()
    expect(await readToken(page, 'refresh_token')).toBeNull()
    expect(await page.evaluate(() => window.localStorage.getItem('cia-auth'))).toBeNull()

    // No refresh storm / redirect loop.
    await page.waitForTimeout(1500)
    expect(redirects).toBeLessThanOrEqual(1)
    await expect(page.getByRole('heading', { name: /^Orders$/ })).toHaveCount(0)
  })
})
