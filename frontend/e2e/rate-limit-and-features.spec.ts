import { test, expect } from '@playwright/test'
import { loginAs, readToken, API_BASE, pageHeading } from './helpers'

// 9.8 429 RATE LIMIT ─────────────────────────────────────────────────────────
// The `e2e` profile keeps `export` at capacity 3 / 60s (login/refresh are high
// so the serial suite can log in freely). An authenticated STAFF firing a few
// rapid exports trips 429 deterministically.
test.describe('9.8 429 rate limiting', () => {
  test('rapid exports → 429 + Retry-After, no logout, no refresh, no retry loop', async ({
    page,
  }) => {
    await loginAs(page, 'STAFF')

    let refreshCalls = 0
    page.on('request', (r) => {
      if (/\/auth\/refresh$/.test(r.url())) refreshCalls += 1
    })

    const results = await page.evaluate(async (base) => {
      const token = localStorage.getItem('access_token')
      const out: { status: number; retryAfter: string | null }[] = []
      for (let i = 0; i < 6; i++) {
        const r = await fetch(`${base}/api/v1/export/products?format=xlsx`, {
          headers: { Authorization: `Bearer ${token}` },
        })
        out.push({ status: r.status, retryAfter: r.headers.get('retry-after') })
        await r.arrayBuffer()
      }
      return out
    }, API_BASE)

    const limited = results.filter((r) => r.status === 429)
    expect(limited.length).toBeGreaterThan(0)
    expect(limited[0].retryAfter).not.toBeNull()
    expect(Number(limited[0].retryAfter)).toBeGreaterThan(0)

    // 429 is not an auth failure.
    expect(refreshCalls).toBe(0)
    expect(await readToken(page, 'access_token')).not.toBeNull()
    await expect(page).not.toHaveURL(/\/login/)
  })
})

// 9.9 IMPORT SECURITY UX ─────────────────────────────────────────────────────
test.describe('9.9 Import security UX', () => {
  test('STAFF gets a safe 403 from the import endpoint (no stack trace)', async ({ page }) => {
    await loginAs(page, 'STAFF')
    const res = await page.evaluate(async (base) => {
      const fd = new FormData()
      fd.append('file', new Blob(['sku,name\n'], { type: 'text/csv' }), 'p.csv')
      const r = await fetch(`${base}/api/v1/import/products`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
        body: fd,
      })
      return { status: r.status, body: await r.text() }
    }, API_BASE)
    expect(res.status).toBe(403)
    expect(res.body).toContain('ACCESS_DENIED')
    expect(res.body).not.toMatch(/org\.springframework|Exception|jdbc:|\bat [\w.$]+\(/)
  })

  test('STAFF sees no upload control on the import page', async ({ page }) => {
    await loginAs(page, 'STAFF')
    await page.goto('/import')
    // The dropzone/file input is never rendered for STAFF, and the page states
    // uploads are Manager/Admin-only.
    await expect(page.locator('input[type="file"]')).toHaveCount(0)
    await expect(page.getByText(/available to Manager and Admin/i)).toBeVisible()
  })

  test('client-side rejects an unsupported extension before any upload request', async ({
    page,
  }) => {
    await loginAs(page, 'MANAGER')
    await page.goto('/import')
    // The dropzone only mounts after an import type is chosen.
    await page.getByRole('button', { name: /product/i }).first().click()

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
    await expect(page.getByText(/unsupported file type|only \.csv and \.xlsx/i).first()).toBeVisible()
    expect(uploadCalls).toBe(0)
  })

  test('rate-limited upload → safe 429, no infinite mutation retry', async ({ page }) => {
    await loginAs(page, 'MANAGER')
    const statuses = await page.evaluate(async (base) => {
      const out: number[] = []
      for (let i = 0; i < 6; i++) {
        const fd = new FormData()
        fd.append('file', new Blob(['bad,data\n1,2\n'], { type: 'text/csv' }), `x${i}.csv`)
        const r = await fetch(`${base}/api/v1/import/products`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
          body: fd,
        })
        out.push(r.status)
        await r.text()
      }
      return out
    }, API_BASE)
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
    await expect(pageHeading(page, /^Orders$/)).toBeVisible()
  })

  test('STAFF creating an order gets a safe 403, stays authenticated', async ({ page }) => {
    await loginAs(page, 'STAFF')
    let refreshCalls = 0
    page.on('request', (r) => {
      if (/\/auth\/refresh$/.test(r.url())) refreshCalls += 1
    })
    // Body is structurally valid (non-empty items, real UUIDs, valid enum) so it
    // clears @Valid and reaches the @PreAuthorize check → a true 403, not a 400.
    const res = await page.evaluate(async (base) => {
      const r = await fetch(`${base}/api/v1/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('access_token')}`,
        },
        body: JSON.stringify({
          customerId: '11111111-1111-1111-1111-111111111111',
          items: [{ productId: '22222222-2222-2222-2222-222222222222', quantity: 1 }],
          paymentMethod: 'CASH',
        }),
      })
      return { status: r.status, body: await r.text() }
    }, API_BASE)
    expect(res.status).toBe(403)
    expect(res.body).toContain('ACCESS_DENIED')
    expect(res.body).not.toMatch(/org\.springframework|Exception|jdbc:|\bat [\w.$]+\(/)
    expect(refreshCalls).toBe(0)
    expect(await readToken(page, 'access_token')).not.toBeNull()
    await expect(page).not.toHaveURL(/\/login/)
  })

  // 9.10 (business conflict / oversell): the `e2e` profile seeds a valid ACTIVE
  // customer + ACTIVE product with only 3 units in stock
  // (backend/src/main/resources/db/e2e/R__seed_e2e_commerce.sql). A MANAGER
  // ordering 50 units hits InventoryService.reserveStock() -> INSUFFICIENT_STOCK.
  //
  // NOTE ON STATUS: the backend maps every BusinessRuleException (incl.
  // INSUFFICIENT_STOCK) to HTTP 422 UNPROCESSABLE_ENTITY. There is no 409 path
  // for overselling and Sprint 13B does not change business rules to invent one
  // (see SPRINT_13B_PRODUCTION_READINESS.md §7). The security-relevant guarantees
  // are identical to a 409: safe normalized message, no stack trace, no refresh,
  // no logout.
  const LOW_STOCK_CUSTOMER = 'e2e00000-0000-0000-0000-000000000001'
  const LOW_STOCK_PRODUCT = 'e2e00000-0000-0000-0000-000000000002'

  test('order oversell surfaces a safe business-conflict message, session intact', async ({
    page,
  }) => {
    await loginAs(page, 'MANAGER')

    let refreshCalls = 0
    let logoutCalls = 0
    page.on('request', (r) => {
      if (/\/auth\/refresh$/.test(r.url())) refreshCalls += 1
      if (/\/auth\/logout$/.test(r.url())) logoutCalls += 1
    })

    const res = await page.evaluate(
      async ({ base, customerId, productId }) => {
        const r = await fetch(`${base}/api/v1/orders`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${localStorage.getItem('access_token')}`,
          },
          body: JSON.stringify({
            customerId,
            items: [{ productId, quantity: 50 }],
            paymentMethod: 'CASH',
          }),
        })
        return { status: r.status, body: await r.text() }
      },
      { base: API_BASE, customerId: LOW_STOCK_CUSTOMER, productId: LOW_STOCK_PRODUCT },
    )

    // Real backend contract: business-rule violation -> 422 (documented; not 409).
    expect(res.status).toBe(422)
    const parsed = JSON.parse(res.body)
    expect(parsed.success).toBe(false)
    expect(parsed.error.code).toBe('INSUFFICIENT_STOCK')
    // The message is safe to show verbatim (lib/apiError treats INSUFFICIENT_STOCK
    // as a trusted code) — it must carry no internal implementation detail.
    expect(parsed.error.message).toMatch(/insufficient stock/i)
    expect(res.body).not.toMatch(/org\.springframework|Exception|jdbc:|\bat [\w.$]+\(|eyJ[A-Za-z0-9_-]{10,}/)

    // A business conflict is NOT an auth failure: no refresh, no logout, no redirect.
    expect(refreshCalls).toBe(0)
    expect(logoutCalls).toBe(0)
    expect(await readToken(page, 'access_token')).not.toBeNull()
    await expect(page).not.toHaveURL(/\/login/)
  })
})
