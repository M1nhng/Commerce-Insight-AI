# Screenshots

Images here are captured from the **running demo stack**
(`./scripts/demo-up.sh`) against the deterministic demo dataset — no real data,
no real credentials, demo users only.

Suggested set (referenced from the root `README.md`):

| File | Screen | Route |
|---|---|---|
| `login.png` | Login | `/login` |
| `dashboard.png` | Dashboard (KPIs, revenue trend, charts, AI card) | `/dashboard` |
| `products.png` | Product catalogue | `/products` |
| `orders.png` | Orders list | `/orders` |
| `analytics.png` | Analytics page | `/analytics` |
| `ai-insights.png` | AI Business Insights card (enabled or degraded state) | `/dashboard` |
| `customers.png` | Customer management | `/customers` |

Current set (captured Sprint 16, 1440×900, dark theme, demo dataset):
`login.png`, `dashboard.png`, `products.png`, `customers.png`, `orders.png`,
`analytics.png`, `ai-insights.png`.

To regenerate: start the demo stack (`./scripts/demo-up.sh`), then drive a headless
browser through `/login` → each route and screenshot it (a short Playwright script
against `http://localhost:5173`, logging in as
`demo-admin@commerceinsight.demo` / `DemoAdmin!2024`). Keep PNGs compressed
(< ~350 KB each) for GitHub.
