import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from '../ProtectedRoute'
import { RoleGuard } from '../RoleGuard'
import { useAuthStore } from '@/store/auth.store'
import type { Role, UserResponse } from '@/types/api.types'

function makeUser(role: Role): UserResponse {
  return {
    id: 'u1', email: 'u@x.com', firstName: 'U', lastName: 'One', fullName: 'U One',
    role, active: true, locked: false, failedAttempts: 0, lastLoginAt: null,
    createdAt: '2026-01-01T00:00:00Z',
  }
}

function setAuth(partial: Partial<ReturnType<typeof useAuthStore.getState>>) {
  useAuthStore.setState({
    user: null, isAuthenticated: false, isInitializing: false, isLoading: false,
    error: null, accessToken: null, refreshToken: null, ...partial,
  })
}

beforeEach(() => setAuth({}))

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={['/secret']}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/secret" element={<div>SECRET CONTENT</div>} />
        </Route>
        <Route path="/login" element={<div>LOGIN PAGE</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  it('redirects an unauthenticated user to /login', () => {
    setAuth({ isAuthenticated: false })
    renderProtected()
    expect(screen.getByText('LOGIN PAGE')).toBeInTheDocument()
    expect(screen.queryByText('SECRET CONTENT')).not.toBeInTheDocument()
  })

  it('does not flash protected content while auth is initializing', () => {
    setAuth({ isAuthenticated: false, isInitializing: true })
    renderProtected()
    expect(screen.queryByText('SECRET CONTENT')).not.toBeInTheDocument()
    expect(screen.queryByText('LOGIN PAGE')).not.toBeInTheDocument()
  })

  it('renders protected content for an authenticated user', () => {
    setAuth({ isAuthenticated: true, user: makeUser('STAFF') })
    renderProtected()
    expect(screen.getByText('SECRET CONTENT')).toBeInTheDocument()
  })
})

describe('RoleGuard', () => {
  function renderGuard(role: Role) {
    setAuth({ isAuthenticated: true, user: makeUser(role) })
    return render(
      <MemoryRouter>
        <RoleGuard roles={['ADMIN']}>
          <div>ADMIN AREA</div>
        </RoleGuard>
      </MemoryRouter>,
    )
  }

  it('shows Access Denied for a STAFF user and does NOT clear auth', () => {
    renderGuard('STAFF')
    expect(screen.getByText('Access Denied')).toBeInTheDocument()
    expect(screen.queryByText('ADMIN AREA')).not.toBeInTheDocument()
    // A 403-style block is not a logout.
    expect(useAuthStore.getState().isAuthenticated).toBe(true)
  })

  it('shows Access Denied for a MANAGER user (no upward implication)', () => {
    renderGuard('MANAGER')
    expect(screen.getByText('Access Denied')).toBeInTheDocument()
  })

  it('renders the guarded content for an ADMIN user', () => {
    renderGuard('ADMIN')
    expect(screen.getByText('ADMIN AREA')).toBeInTheDocument()
  })
})
