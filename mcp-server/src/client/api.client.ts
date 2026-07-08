import axios, { AxiosInstance, AxiosResponse } from 'axios';
import { config } from '../config/index.js';
import { logger } from '../utils/logger.js';

/**
 * Standard backend response envelope
 */
export interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

/**
 * Creates and configures the Axios client for the Spring Boot backend
 */
export const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    baseURL: config.backendApiUrl,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-MCP-API-KEY': config.mcpApiKey, // Injected for backend authentication
    },
  });

  // Request interceptor for logging
  client.interceptors.request.use(
    (request) => {
      logger.debug(`Outgoing request: ${request.method?.toUpperCase()} ${request.url}`);
      return request;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // Response interceptor to unwrap data and handle standard errors
  client.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      logger.debug(`Response received: ${response.status} from ${response.config.url}`);
      // Return the inner data directly for convenience
      return response.data as any;
    },
    (error) => {
      // Let the caller handle the error via error.handler.ts
      return Promise.reject(error);
    }
  );

  return client;
};

// Export a singleton instance
export const apiClient = createApiClient();
