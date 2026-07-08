import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { config } from './config/index.js';
import { logger } from './utils/logger.js';
import { ProviderRegistry } from './providers/registry.js';
import { apiClient } from './client/api.client.js';

export class CommerceInsightMcpServer {
  private server: McpServer;
  private registry: ProviderRegistry;

  constructor() {
    // Initialize the MCP SDK Server
    this.server = new McpServer({
      name: 'commerce-insight-ai',
      version: '1.0.0',
    });

    this.registry = new ProviderRegistry();
  }

  /**
   * Used to register tools, resources, and prompts via providers.
   * This allows modularly adding capabilities to the server.
   */
  public getRegistry(): ProviderRegistry {
    return this.registry;
  }

  /**
   * Starts the MCP server and connects it to the configured transport.
   */
  public async start(): Promise<void> {
    try {
      // 1. Initialize all registered providers
      this.registry.initializeAll(this.server, apiClient);

      // 2. Setup and connect the transport
      if (config.transport === 'stdio') {
        const transport = new StdioServerTransport();
        await this.server.connect(transport);
        logger.info('MCP Server started on STDIO transport');
      } else {
        // SSE transport implementation would go here (e.g. with Express)
        logger.warn(`Transport '${config.transport}' is currently unsupported. Defaulting to stdio.`);
        const transport = new StdioServerTransport();
        await this.server.connect(transport);
        logger.info('MCP Server started on STDIO transport (fallback)');
      }

    } catch (error) {
      logger.error('Failed to start MCP Server', { error });
      process.exit(1);
    }
  }
}
