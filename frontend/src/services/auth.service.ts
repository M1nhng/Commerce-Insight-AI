/**
 * services/auth.service.ts — Auth API calls.
 *
 * Architecture: This module contains ONLY API call functions.
 * No state management here — that is Zustand's job (store/auth.store.ts).
 */
import apiClient from './axios'
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
} from '@/types/api.types'

const AUTH_BASE = '/auth'

export const authService = {
  /**
   * POST /api/v1/auth/login
   */
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>(
      `${AUTH_BASE}/login`,
      data
    )
    if (!response.data.data) throw new Error('Login failed: empty response')
    return response.data.data
  },

  /**
   * POST /api/v1/auth/register
   */
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>(
      `${AUTH_BASE}/register`,
      data
    )
    if (!response.data.data) throw new Error('Register failed: empty response')
    return response.data.data
  },

  /**
   * POST /api/v1/auth/refresh
   */
  async refresh(refreshToken: string): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>(
      `${AUTH_BASE}/refresh`,
      { refreshToken }
    )
    if (!response.data.data) throw new Error('Refresh failed: empty response')
    return response.data.data
  },

  /**
   * POST /api/v1/auth/logout
   */
  async logout(): Promise<void> {
    await apiClient.post(`${AUTH_BASE}/logout`)
  },

  /**
   * GET /api/v1/auth/me
   */
  async getCurrentUser(): Promise<UserResponse> {
    const response = await apiClient.get<ApiResponse<UserResponse>>(
      `${AUTH_BASE}/me`
    )
    if (!response.data.data) throw new Error('GetMe failed: empty response')
    return response.data.data
  },

  /**
   * GET /api/v1/auth/verify
   * Lightweight token introspection — returns the current user if the token is valid.
   * Returns null on 401 instead of throwing.
   */
  async verifyToken(): Promise<UserResponse | null> {
    try {
      const response = await apiClient.get<ApiResponse<UserResponse>>(
        `${AUTH_BASE}/verify`
      )
      return response.data.data ?? null
    } catch {
      return null
    }
  },
}
