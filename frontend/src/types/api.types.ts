/**
 * API types — shared type definitions mirroring the Spring Boot DTOs.
 * These must stay in sync with the backend API specification.
 */

// ── Generic API Response Envelope ─────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T | null;
  error: ApiError | null;
  timestamp: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}

// ── Auth Types ────────────────────────────────────────────────────────────

export type Role = 'ADMIN' | 'MANAGER' | 'STAFF';

export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  role: Role;
  active: boolean;
  locked: boolean;
  failedAttempts: number;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserResponse | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

// ── Pagination ────────────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
