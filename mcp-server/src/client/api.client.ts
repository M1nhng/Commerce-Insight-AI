/**
 * API Client — HTTP client for communicating with the Spring Boot REST API.
 *
 * RULE: The MCP server NEVER accesses the database directly.
 *       ALL data flows through this client → Spring Boot REST API.
 *
 * TODO: Implement when MCP phase begins (see docs/09_MCP.md):
 *   - Axios instance with base URL from config
 *   - Auth header injection (MCP_API_KEY)
 *   - Request/response interceptors for logging
 *   - Error handling with typed ApiError
 */

import { config } from '../config/index.js'

// Placeholder export — will be replaced with Axios instance
export const apiClient = {
  baseUrl: config.backendApiUrl,
  // TODO: Initialize axios instance
  // TODO: Add auth interceptor
  // TODO: Add error interceptor
}
