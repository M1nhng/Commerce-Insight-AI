/**
 * Commerce Insight AI — MCP Server Entry Point
 *
 * This server implements the Model Context Protocol (MCP) to allow AI agents
 * to interact with Commerce Insight AI data through well-defined tools.
 *
 * CRITICAL CONSTRAINT:
 *   This server NEVER accesses the database directly.
 *   All data is fetched via the Spring Boot REST API.
 */

import { config } from './config/index.js';
import { logger } from './utils/logger.js';
import { CommerceInsightMcpServer } from './server.js';
import { startHealthCheckServer } from './health.js';
import { ProductToolsProvider }  from './tools/products.tool.js';
import { CategoryToolsProvider } from './tools/categories.tool.js';
import { InventoryToolsProvider } from './tools/inventory.tool.js';
import { CustomerToolsProvider } from './tools/customer.tool.js';
import { OrderToolsProvider }    from './tools/orders.tool.js';
import { AnalyticsToolsProvider } from './tools/analytics.tool.js';
import { ImportToolsProvider }   from './tools/import.tool.js';

// Setup global error handlers for uncaught exceptions
process.on('uncaughtException', (error) => {
  logger.error('Uncaught Exception', { error });
  process.exit(1);
});

process.on('unhandledRejection', (reason) => {
  logger.error('Unhandled Rejection', { reason });
});

async function main() {
  logger.info(`Commerce Insight AI MCP Server starting...`);
  logger.info(`Backend API: ${config.backendApiUrl}`);
  logger.info(`Log Level: ${config.logLevel.toUpperCase()}`);

  // 1. Start the health check server
  startHealthCheckServer();

  // 2. Initialize the MCP Server
  const mcpServer = new CommerceInsightMcpServer();

  // 3. Register Providers
  const registry = mcpServer.getRegistry();
  registry.registerProvider(new ProductToolsProvider());
  registry.registerProvider(new CategoryToolsProvider());
  registry.registerProvider(new InventoryToolsProvider());
  registry.registerProvider(new CustomerToolsProvider());
  registry.registerProvider(new OrderToolsProvider());
  registry.registerProvider(new AnalyticsToolsProvider());
  registry.registerProvider(new ImportToolsProvider());

  // 4. Connect and start
  await mcpServer.start();
}

main().catch((error) => {
  logger.error('Fatal error during startup', { error });
  process.exit(1);
});
