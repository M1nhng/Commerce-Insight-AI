import { z } from 'zod';

const configSchema = z.object({
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  PORT: z.string().transform(Number).default('3001'),
  BACKEND_API_URL: z.string().url().default('http://localhost:8080/api/v1'),
  MCP_API_KEY: z.string().min(1, "MCP_API_KEY is required"),
  LOG_LEVEL: z.enum(['debug', 'info', 'warn', 'error']).default('info'),
  MCP_TRANSPORT: z.enum(['stdio', 'sse']).default('stdio'),
});

// Validate process.env
const _config = configSchema.safeParse(process.env);

if (!_config.success) {
  console.error('❌ Invalid environment variables:', _config.error.format());
  process.exit(1);
}

export const config = {
  env: _config.data.NODE_ENV,
  port: _config.data.PORT,
  backendApiUrl: _config.data.BACKEND_API_URL,
  mcpApiKey: _config.data.MCP_API_KEY,
  logLevel: _config.data.LOG_LEVEL,
  transport: _config.data.MCP_TRANSPORT,
};
