/**
 * MCP Server Configuration
 *
 * Loads and validates environment variables.
 * All config values are accessed through this module — never directly from process.env.
 */

export const config = {
  port: parseInt(process.env.PORT ?? '3001', 10),
  backendApiUrl: process.env.BACKEND_API_URL ?? 'http://localhost:8080/api',
  mcpApiKey: process.env.MCP_API_KEY ?? '',
  logLevel: process.env.LOG_LEVEL ?? 'info',
} as const

// Validate required fields
if (!config.mcpApiKey) {
  console.warn('[Config] WARNING: MCP_API_KEY is not set. Backend requests will be unauthenticated.')
}
