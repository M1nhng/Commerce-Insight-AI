/**
 * src/test/setup.ts — Vitest global setup.
 *
 * - jest-dom matchers
 * - a functional localStorage in jsdom
 * - reset storage + module state between tests
 */
import '@testing-library/jest-dom/vitest'
import { afterEach, vi } from 'vitest'

// jsdom ships a localStorage, but make it deterministic + resettable.
class MemoryStorage {
  private store = new Map<string, string>()
  get length() {
    return this.store.size
  }
  clear() {
    this.store.clear()
  }
  getItem(key: string) {
    return this.store.has(key) ? this.store.get(key)! : null
  }
  setItem(key: string, value: string) {
    this.store.set(key, String(value))
  }
  removeItem(key: string) {
    this.store.delete(key)
  }
  key(index: number) {
    return Array.from(this.store.keys())[index] ?? null
  }
}

Object.defineProperty(window, 'localStorage', {
  value: new MemoryStorage(),
  writable: true,
})

// Silence react-hot-toast's DOM work in unit tests.
vi.mock('react-hot-toast', () => {
  const fn = Object.assign(vi.fn(), {
    success: vi.fn(),
    error: vi.fn(),
    loading: vi.fn(),
    dismiss: vi.fn(),
    custom: vi.fn(),
  })
  return { default: fn, toast: fn, Toaster: () => null }
})

afterEach(() => {
  window.localStorage.clear()
  vi.clearAllMocks()
})
