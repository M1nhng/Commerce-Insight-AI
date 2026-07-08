import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from './provider.interface.js';
import { logger } from '../utils/logger.js';

/**
 * Registry to manage and initialize all MCP providers.
 */
export class ProviderRegistry {
  private providers: McpProvider[] = [];

  /**
   * Add a new provider to the registry.
   */
  public registerProvider(provider: McpProvider): void {
    this.providers.push(provider);
  }

  /**
   * Initializes all registered providers by injecting the server and API client.
   * 
   * @param server The MCP Server instance
   * @param apiClient The Axios client
   */
  public initializeAll(server: McpServer, apiClient: AxiosInstance): void {
    logger.info(`Initializing ${this.providers.length} provider(s)...`);
    
    for (const provider of this.providers) {
      try {
        provider.register(server, apiClient);
        logger.debug(`Successfully registered provider: ${provider.constructor.name || 'AnonymousProvider'}`);
      } catch (error) {
        logger.error(`Failed to register provider`, { error });
        throw error;
      }
    }

    logger.info('All providers initialized successfully.');
  }
}
