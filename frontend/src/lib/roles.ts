/**
 * lib/roles.ts — Single source of truth for the client-side role hierarchy.
 *
 * Mirrors the backend Sprint 12A RoleHierarchy: ADMIN > MANAGER > STAFF.
 *
 * IMPORTANT: these checks are UI gating only. The backend remains the sole
 * authority on authorization — a user who bypasses a hidden button still gets
 * a 403 from the API.
 */
import type { Role } from '@/types/api.types'

/** Higher number = more privilege. */
export const ROLE_LEVELS: Record<Role, number> = {
  STAFF: 1,
  MANAGER: 2,
  ADMIN: 3,
}

/** True when `role` meets or exceeds `minimumRole` in the hierarchy. */
export function isAtLeast(role: Role | null | undefined, minimumRole: Role): boolean {
  if (!role || !(role in ROLE_LEVELS)) return false
  return ROLE_LEVELS[role] >= ROLE_LEVELS[minimumRole]
}

/**
 * True when `role` is one of `allowed`, honouring the hierarchy: listing
 * MANAGER also admits ADMIN. Never trusts an unknown role string.
 */
export function hasRole(role: Role | null | undefined, allowed: Role[]): boolean {
  if (!role || !(role in ROLE_LEVELS)) return false
  if (allowed.length === 0) return false
  const minLevel = Math.min(...allowed.map((r) => ROLE_LEVELS[r]))
  return ROLE_LEVELS[role] >= minLevel
}
