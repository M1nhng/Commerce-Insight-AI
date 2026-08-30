import { describe, it, expect, beforeEach } from 'vitest'
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
  hasTokens,
} from '../authTokens'

beforeEach(() => window.localStorage.clear())

describe('authTokens', () => {
  it('round-trips the token pair', () => {
    expect(hasTokens()).toBe(false)
    setTokens('access-1', 'refresh-1')
    expect(getAccessToken()).toBe('access-1')
    expect(getRefreshToken()).toBe('refresh-1')
    expect(hasTokens()).toBe(true)
  })

  it('clearTokens wipes the pair, the persisted store blob, and legacy keys', () => {
    setTokens('a', 'b')
    window.localStorage.setItem('cia-auth', '{"state":{"isAuthenticated":true}}')
    window.localStorage.setItem('user', '{"id":"1"}')

    clearTokens()

    expect(getAccessToken()).toBeNull()
    expect(getRefreshToken()).toBeNull()
    expect(window.localStorage.getItem('cia-auth')).toBeNull()
    expect(window.localStorage.getItem('user')).toBeNull()
  })

  it('is safe to call clearTokens repeatedly', () => {
    expect(() => {
      clearTokens()
      clearTokens()
    }).not.toThrow()
  })
})
