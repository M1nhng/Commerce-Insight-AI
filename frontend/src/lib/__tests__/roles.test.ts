import { describe, it, expect } from 'vitest'
import { isAtLeast, hasRole, ROLE_LEVELS } from '../roles'

describe('roles — hierarchy (ADMIN > MANAGER > STAFF)', () => {
  it('ROLE_LEVELS orders privilege ascending', () => {
    expect(ROLE_LEVELS.ADMIN).toBeGreaterThan(ROLE_LEVELS.MANAGER)
    expect(ROLE_LEVELS.MANAGER).toBeGreaterThan(ROLE_LEVELS.STAFF)
  })

  it('isAtLeast is hierarchy-aware', () => {
    expect(isAtLeast('ADMIN', 'MANAGER')).toBe(true)
    expect(isAtLeast('MANAGER', 'MANAGER')).toBe(true)
    expect(isAtLeast('STAFF', 'MANAGER')).toBe(false)
    expect(isAtLeast('ADMIN', 'ADMIN')).toBe(true)
    expect(isAtLeast('MANAGER', 'ADMIN')).toBe(false)
  })

  it('isAtLeast rejects missing / unknown roles', () => {
    expect(isAtLeast(null, 'STAFF')).toBe(false)
    expect(isAtLeast(undefined, 'STAFF')).toBe(false)
    // @ts-expect-error — deliberately invalid role string
    expect(isAtLeast('SUPERADMIN', 'STAFF')).toBe(false)
  })

  it('hasRole admits a listed role and every role above it', () => {
    expect(hasRole('ADMIN', ['MANAGER'])).toBe(true) // ADMIN implies MANAGER
    expect(hasRole('MANAGER', ['MANAGER', 'ADMIN'])).toBe(true)
    expect(hasRole('STAFF', ['MANAGER', 'ADMIN'])).toBe(false)
    expect(hasRole('STAFF', [])).toBe(false)
    expect(hasRole(null, ['STAFF'])).toBe(false)
  })
})
