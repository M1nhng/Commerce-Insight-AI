import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';

/**
 * Standard interface for all MCP providers (tools, resources, prompts).
 * Every provider module MUST implement this interface to be registered.
 */
export interface McpProvider {
  /**
   * Registers the provider's features onto the given MCP Server instance.
   * 
   * @param server The MCP Server instance
   * @param apiClient The configured Axios client for making backend requests
   */
  register(server: McpServer, apiClient: AxiosInstance): void;
}
