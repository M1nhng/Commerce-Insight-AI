import { defineConfig, devices } from '@playwright/test'

/**
 * Playwright config for Sprint 12C end-to-end security verification.
 *
 * The full stack is expected to be running already (see docker-compose.e2e.yml):
 *   docker compose -f docker-compose.yml -f docker-compose.e2e.yml up -d --build
 *
 * so this config does NOT start a webServer. Point it elsewhere with E2E_BASE_URL.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // security flows share rate-limit buckets — keep serial
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: process.env.CI ? [['github'], ['list']] : [['list']],
  timeout: 30_000,
  expect: { timeout: 7_000 },
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    actionTimeout: 10_000,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
