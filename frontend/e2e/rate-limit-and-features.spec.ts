import { test, expect } from '@playwright/test'
import { loginAs, readToken, USERS } from './helpers'

// 9.8 429 RATE LIMIT ─────────────────────────────────────────────────────────
// The `e2e` backend profile sets app.rate-limit.login.capacity=4 / 60s so this
// trips in a handful of requests instead of a full minute.
test.describe('9.8 429 rate limiting', () => {
  test('rapid bad logins → 429 with Retry-After, no logout, no auto-retry loop', async ({
    page,
  }) => {
    await page.goto('/login')

    const results = await page.evaluate(async () => {
      const out: { status: number; retryAfter: string | null }[] = []
      for (let i = 0; i < 8; i++) {
        const r = await fetch('/api/v1/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: 'e2e-staff@commerceinsight.test', password: 'Wrong!000' }),
        })
        out.push({ status: r.status, retryAfter: r.headers.get('retry-after') })
        await r.text()
      }
      return out
    })

    const limited = results.filter((r) => r.status === 429)
    expect(limited.length).toBeGreaterThan(0)
    expect(limited[0].retryAfter).not.toBeNull()
    expect(Number(limited[0].retryAfter)).toBeGreaterThan(0)

    // Still on the login page, no token, app not broken.
    await expect(page).toHaveURL(/\/login/)
    expect(await readToken(page, 'access_token')).toBeNull()

    // Driving the UI once more surfaces a safe rate-limit message (no stack trace).
    await page.locator('#email').fill(USERS.STAFF.email)
    await page.locator('#password').fill('Wrong!000')
    await page.getByRole('button', { name: /sign in/i }).click()
    const alert = page.getByRole('alert')
    await expect(alert).toBeVisible()
    await expect(alert).toContainText(/too many requests/i)
  })
})

// 9.9 IMPORT SECURITY UX ─────────────────────────────────────────────────────
test.describe('9.9 Import security UX', () => {
  test('STAFF gets a safe 403 from the import endpoint (no stack trace)', async ({ page }) => {
    await loginAs(page, 'STAFF')
    const res = await page.evaluate(async () => {
      const fd = new FormData()
      fd.append('file', new Blob(['sku,name\n'], { type: 'text/csv' }), 'p.csv')
      const r = await fetch('/api/v1/import/products', {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
        body: fd,
      })
      return { status: r.status, body: await r.text() }
    })
    expect(res.status).toBe(403)
    expect(res.body).toContain('ACCESS_DENIED')
    expect(res.body).not.toMatch(/org\.springframework|Exception|jdbc:|\bat [\w.$]+\(/)
  })

  test('STAFF sees no upload control on the import page', async ({ page }) => {
    await loginAs(page, 'STAFF')
    await page.goto('/import')
    await expect(page.getByText(/only managers and admins/i)).toBeVisible()
  })

  test('client-side rejects an unsupported extension before any upload request', async ({
    page,
  }) => {
    await loginAs(page, 'MANAGER')
    await page.goto('/import')
    let uploadCalls = 0
    page.on('request', (r) => {
      if (/\/import\/(products|customers|orders)$/.test(r.url()) && r.method() === 'POST') {
        uploadCalls += 1
      }
    })
    await page.locator('input[type="file"]').setInputFiles({
      name: 'notes.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('hello'),
    })
    await expect(page.getByText(/csv|xlsx|unsupported|not a valid/i).first()).toBeVisible()
    expect(uploadCalls).toBe(0)
  })

  test('rate-limited upload → safe 429, no infinite mutation retry', async ({ page }) => {
    await loginAs(page, 'MANAGER')
    const statuses = await page.evaluate(async () => {
      const out: number[] = []
      for (let i = 0; i < 6; i++) {
        const fd = new FormData()
        fd.append('file', new Blob(['bad,data\n1,2\n'], { type: 'text/csv' }), `x${i}.csv`)
        const r = await fetch('/api/v1/import/products', {
          method: 'POST',
          headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
          body: fd,
        })
        out.push(r.status)
        await r.text()
      }
      return out
    })
    expect(statuses).toContain(429)
    await expect(page).not.toHaveURL(/\/login/)
  })
})

// 9.10 ORDER SECURITY UX ─────────────────────────────────────────────────────
test.describe('9.10 Order security UX', () => {
  test('MANAGER can open the orders list (read path works)', async ({ page }) => {
    await loginAs(page, 'MANAGER')
    await page.goto('/orders')
    await expect(page).toHaveURL(/\/orders/)
    await expect(page.getByRole('heading', { name: /^Orders$/ })).toBeVisible()
  })

  test('STAFF creating an order gets a safe 403, stays authenticated', async ({ page }) => {
    await loginAs(page, 'STAFF')
    let refreshCalls = 0
    page.on('request', (r) => {
      if (/\/auth\/refresh$/.test(r.url())) refreshCalls += 1
    })
    const res = await page.evaluate(async () => {
      const r = await fetch('/api/v1/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({ customerId: '00000000-0000-0000-0000-000000000000', items: [] }),
      })
      return { status: r.status, body: await r.text() }
    })
    expect(res.status).toBe(403)
    expect(res.body).toContain('ACCESS_DENIED')
    expect(refreshCalls).toBe(0)
    expect(await readToken(page, 'access_token')).not.toBeNull()
    await expect(page).not.toHaveURL(/\/login/)
  })

  // 9.10 (409 business conflict): requires seeded customer + product + stock to
  // deterministically provoke an oversell. Deferred until a commerce E2E seed
  // exists — see SPRINT_12C_SECURITY_VERIFICATION.md §Deferred.
  test.fixme('order creation surfaces a safe 409 business-conflict message', async () => {})
})
