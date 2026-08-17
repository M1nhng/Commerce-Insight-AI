import { AxiosError } from 'axios';
import { logger } from './logger.js';

export class McpError extends Error {
  public code: string;

  constructor(message: string, code: string = 'InternalError') {
    super(message);
    this.name = 'McpError';
    this.code = code;
  }
}

export function handleApiError(error: unknown, context: string): McpError {
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    const backendMessage = error.response?.data?.message || error.message;

    logger.error(`API Error in ${context}`, {
      status,
      message: backendMessage,
      url: error.config?.url,
    });

    if (status === 401 || status === 403) {
      return new McpError(`Authentication failed with backend API: ${backendMessage}`, 'InvalidRequest');
    }
    if (status === 400 || status === 422) {
      return new McpError(`Invalid request to backend API: ${backendMessage}`, 'InvalidParams');
    }
    if (status === 404) {
      return new McpError(`Resource not found: ${backendMessage}`, 'InvalidRequest');
    }
    if (status === 429) {
      return new McpError(`Rate limited by backend API. Please try again later.`, 'InternalError');
    }
    
    return new McpError(`Backend service error: ${backendMessage}`, 'InternalError');
  }

  if (error instanceof Error) {
    logger.error(`Unexpected error in ${context}`, { message: error.message, stack: error.stack });
    return new McpError(`Unexpected error: ${error.message}`, 'InternalError');
  }

  logger.error(`Unknown error in ${context}`, { error });
  return new McpError('An unknown error occurred', 'InternalError');
}

/**
 * Formats any error into a standard MCP text content array
 */
export function formatErrorForMcp(error: unknown): { content: { type: 'text', text: string }[], isError: true } {
  const message = error instanceof Error ? error.message : String(error);
  return {
    content: [{ type: 'text', text: `Error: ${message}` }],
    isError: true,
  };
}

// ── Shared rich MCP error formatter ──────────────────────────────────────────
//
// Converts an Axios error (or any unknown error) into a human-readable MCP
// error response that an LLM can communicate naturally to the user.
//
// Rules enforced here (§7.4 Security Design):
//   - Never exposes stack traces
//   - Never exposes DB connection strings, SQL, or passwords
//   - Never exposes JWT tokens or API keys
//   - Messages are end-user-safe

export type McpToolError = {
  content: { type: 'text'; text: string }[];
  isError: true;
};

/**
 * Converts an Axios error or unknown error into a structured MCP tool error response.
 *
 * @param toolName  - Name of the MCP tool (used in error message prefix for LLM context)
 * @param error     - The caught error (Axios or unknown)
 */
export function toMcpToolError(toolName: string, error: unknown): McpToolError {
  if (error instanceof AxiosError) {
    const status = error.response?.status;
    const apiMsg =
      (error.response?.data as any)?.error?.message ??
      (error.response?.data as any)?.message ??
      error.message;

    // Network error — backend unreachable
    if (!error.response) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: Unable to reach the Commerce Insight backend. Please ensure the API server is running.`,
        }],
        isError: true,
      };
    }

    if (status === 400) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: Invalid request — ${apiMsg}`,
        }],
        isError: true,
      };
    }

    if (status === 401 || status === 403) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: Access denied. The MCP service key may be invalid or expired.`,
        }],
        isError: true,
      };
    }

    if (status === 404) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: The requested resource was not found. ${apiMsg}`,
        }],
        isError: true,
      };
    }

    if (status === 409) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: Order could not be created because inventory or another business constraint was violated. ${apiMsg}`,
        }],
        isError: true,
      };
    }

    if (status === 429) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: Rate limit exceeded. Please wait a moment before retrying.`,
        }],
        isError: true,
      };
    }

    if (status && status >= 500) {
      return {
        content: [{
          type: 'text',
          text: `Error [${toolName}]: The Commerce Insight backend encountered an internal error (HTTP ${status}). Please try again shortly.`,
        }],
        isError: true,
      };
    }

    return {
      content: [{
        type: 'text',
        text: `Error [${toolName}]: Unexpected API error (HTTP ${status ?? 'unknown'}). ${apiMsg}`,
      }],
      isError: true,
    };
  }

  return {
    content: [{
      type: 'text',
      text: `Error [${toolName}]: An unexpected error occurred. Please try again.`,
    }],
    isError: true,
  };
}
