import { test, expect } from '@playwright/test'
import { loginAs, API_BASE } from './helpers'

/**
 * Sprint 13D — Dashboard (analytics) rendering against a REAL backend.
 *
 * These tests need a stack whose database actually has orders/revenue. Run them
 * against the Sprint 13C demo stack:
 *
 *   docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d
 *   E2E_ADMIN_EMAIL=demo-admin@commerceinsight.demo \
 *   E2E_ADMIN_PASSWORD='DemoAdmin!2024' \
 *   npx playwright test dashboard.spec.ts
 *
 * They are separate from the Sprint 12C auth/RBAC/refresh suite (which targets
 * the e2e stack + its own seed users) and do not duplicate it.
 */
test.describe('13D Dashboard', () => {
  test('ADMIN lands on the dashboard and sees non-zero KPI + chart data', async ({ page }) => {
    const analyticsCalls: number[] = []
    page.on('response', (r) => {
      if (/\/api\/v1\/analytics\//.test(r.url())) analyticsCalls.push(r.status())
    })

    await loginAs(page, 'ADMIN')
    // Sprint 13D: '/' and '/dashboard' render the analytics dashboard.
    await expect(page).toHaveURL(/\/(dashboard)?$/)

    // KPI cards render (labels appear elsewhere too — scope to the first match).
    await expect(page.getByText('Total Revenue').first()).toBeVisible()
    await expect(page.getByText('Total Orders').first()).toBeVisible()
    await expect(page.getByText('Products Sold').first()).toBeVisible()

    // Every analytics request the page fired came back 200 (no 500, no NULL-bind).
    await expect.poll(() => analyticsCalls.length, { timeout: 15_000 }).toBeGreaterThan(0)
    expect(analyticsCalls.every((s) => s === 200)).toBe(true)

    // KPI values resolve to real formatted numbers (loading skeleton gone, no "—").
    await expect(page.locator('p.text-xl.font-bold').first()).toContainText(/[1-9]/, { timeout: 15_000 })

    // The revenue trend chart renders an SVG once data arrives.
    await expect(page.locator('.recharts-responsive-container').first()).toBeVisible({ timeout: 15_000 })

    // No backend internals leak into the DOM.
    const body = await page.locator('body').innerText()
    expect(body).not.toMatch(/could not determine data type|SQLState|org\.springframework|jdbc:|\bat [\w.$]+\(/)
    expect(body).not.toMatch(/eyJ[A-Za-z0-9_-]{10,}/)
  })

  test('overview API returns real seeded totals through the running stack', async ({ page }) => {
    await loginAs(page, 'ADMIN')
    const res = await page.evaluate(async (base) => {
      const r = await fetch(`${base}/api/v1/analytics/overview`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('access_token')}` },
      })
      return { status: r.status, body: (await r.json()) as { data?: Record<string, number> } }
    }, API_BASE)

    expect(res.status).toBe(200)
    expect(res.body.data?.totalRevenue ?? 0).toBeGreaterThan(0)
    expect(res.body.data?.totalOrders ?? 0).toBeGreaterThan(0)
    expect(res.body.data?.totalCustomers ?? 0).toBeGreaterThan(0)
  })

  test('a failing analytics request degrades to a safe per-section message', async ({ page }) => {
    // Force one endpoint to 500; the page must show a safe message, not crash.
    await page.route('**/api/v1/analytics/overview*', (route) =>
      route.fulfill({ status: 500, contentType: 'application/json', body: '{"success":false,"error":{"code":"INTERNAL_ERROR","message":"boom"}}' }),
    )
    await loginAs(page, 'ADMIN')
    await expect(page.getByText(/Unable to load overview data/i)).toBeVisible({ timeout: 15_000 })
    // Other sections still work — the whole page did not blank out.
    await expect(page.getByText('Order Status')).toBeVisible()
    const body = await page.locator('body').innerText()
    expect(body).not.toMatch(/could not determine data type|org\.springframework|\bat [\w.$]+\(/)
  })

  test('breadcrumb and page heading agree on /dashboard and on /analytics — Sprint 14', async ({ page }) => {
    await loginAs(page, 'ADMIN')

    // Landing route: header breadcrumb and the in-page <h1> both say "Dashboard".
    await expect(page.locator('header h1')).toHaveText('Dashboard')
    await expect(page.locator('#main-content h1')).toHaveText('Dashboard')

    // The legacy /analytics route reuses the same page but is labelled "Analytics".
    await page.getByRole('link', { name: 'Analytics' }).click()
    await expect(page).toHaveURL(/\/analytics$/)
    await expect(page.locator('header h1')).toHaveText('Analytics')
    await expect(page.locator('#main-content h1')).toHaveText('Analytics')
  })

  test('a lazy-loaded protected route still redirects an unauthenticated visitor', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/login')
    await page.evaluate(() => {
      try {
        localStorage.clear()
        sessionStorage.clear()
      } catch {
        /* ignore */
      }
    })

    // /orders is React.lazy'd (Sprint 14). Guard must resolve before the chunk mounts.
    await page.goto('/orders')
    await expect(page).toHaveURL(/\/login$/)
    const body = await page.locator('body').innerText()
    expect(body).not.toMatch(/Orders list|Create Order/i)
  })
})

/**
 * AI Business Insights — the POST /api/v1/analytics/ai-insights endpoint is
 * intercepted so the suite is deterministic and never needs a real LLM key.
 */
const AI_ROUTE = '**/api/v1/analytics/ai-insights'

test.describe('AI Business Insights', () => {
  test('generate → loading → structured insights, no backend internals leak', async ({ page }) => {
    await page.route(AI_ROUTE, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            available: true,
            summary: 'Revenue rose versus the previous period, driven by a few products.',
            insights: [
              { type: 'POSITIVE', title: 'Revenue growth', description: 'Revenue is up on the prior window.', metric: '+23%', severity: 'HIGH' },
              { type: 'WARNING', title: 'Inventory risk', description: 'Several products are low on stock.', metric: '9 items', severity: 'MEDIUM' },
            ],
            recommendations: [
              { title: 'Review low-stock products', description: 'Reorder the flagged SKUs.', priority: 'HIGH' },
            ],
            generatedAt: new Date().toISOString(),
            provider: 'openai',
            model: 'gpt-4o-mini',
          },
        }),
      }),
    )

    await loginAs(page, 'ADMIN')
    await expect(page.getByRole('heading', { name: 'AI Business Insights' })).toBeVisible()

    await page.getByRole('button', { name: /generate ai insights/i }).click()
    await expect(page.getByText(/revenue rose versus the previous period/i)).toBeVisible({ timeout: 15_000 })
    await expect(page.getByText('Revenue growth')).toBeVisible()
    await expect(page.getByText('Review low-stock products')).toBeVisible()
    await expect(page.getByText(/recommendations are AI suggestions/i)).toBeVisible()

    const body = await page.locator('body').innerText()
    expect(body).not.toMatch(/org\.springframework|jdbc:|\bat [\w.$]+\(|eyJ[A-Za-z0-9_-]{10,}/)
  })

  test('available:false → calm "unavailable" message, dashboard still works', async ({ page }) => {
    await page.route(AI_ROUTE, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { available: false, summary: 'AI insights are temporarily unavailable.', insights: [], recommendations: [], generatedAt: new Date().toISOString(), provider: null, model: null },
        }),
      }),
    )

    await loginAs(page, 'ADMIN')
    await page.getByRole('button', { name: /generate ai insights/i }).click()
    await expect(page.getByText(/temporarily unavailable/i)).toBeVisible({ timeout: 15_000 })
    // The real analytics sections are unaffected.
    await expect(page.getByText('Total Revenue').first()).toBeVisible()
    await expect(page.locator('.recharts-responsive-container').first()).toBeVisible()
  })

  test('403 on AI generation → safe message, session intact', async ({ page }) => {
    await page.route(AI_ROUTE, (route) =>
      route.fulfill({ status: 403, contentType: 'application/json', body: '{"success":false,"error":{"code":"ACCESS_DENIED","message":"forbidden"}}' }),
    )
    await loginAs(page, 'ADMIN')
    await page.getByRole('button', { name: /generate ai insights/i }).click()
    await expect(page.getByText(/do not have permission/i)).toBeVisible({ timeout: 15_000 })
    await expect(page).toHaveURL(/\/(dashboard)?$/) // not logged out
  })

  test('429 on AI generation → rate-limit message, no logout', async ({ page }) => {
    await page.route(AI_ROUTE, (route) =>
      route.fulfill({
        status: 429,
        contentType: 'application/json',
        headers: { 'Retry-After': '30' },
        // Mirrors the real RateLimitingFilter envelope.
        body: '{"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please retry after 30 second(s)."}}',
      }),
    )
    await loginAs(page, 'ADMIN')
    await page.getByRole('button', { name: /generate ai insights/i }).click()
    await expect(page.getByText(/too many requests/i)).toBeVisible({ timeout: 15_000 })
    await expect(page).toHaveURL(/\/(dashboard)?$/)
  })
})
