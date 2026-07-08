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
