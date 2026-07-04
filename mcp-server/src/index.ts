/**
 * Commerce Insight AI — MCP Server Entry Point
 *
 * This server implements the Model Context Protocol (MCP) to allow AI agents
 * to interact with Commerce Insight AI data through well-defined tools.
 *
 * CRITICAL CONSTRAINT:
 *   This server NEVER accesses the database directly.
 *   All data is fetched via the Spring Boot REST API.
 *
 * Architecture:
 *   AI Agent → MCP Protocol → This Server → HTTP REST → Spring Boot API → PostgreSQL
 *
 * TODO: Implement when MCP phase begins (see docs/09_MCP.md):
 *   1. Initialize MCP Server with SDK
 *   2. Register all tools (analytics, products, orders, ai)
 *   3. Register resources and prompts
 *   4. Configure transport (stdio or SSE)
 */

import 'dotenv/config'
import { config } from './config/index.js'

// Placeholder — server initialization will go here
console.log(`Commerce Insight AI MCP Server`)
console.log(`Backend API: ${config.backendApiUrl}`)
console.log(`Port: ${config.port}`)
console.log(`Status: 🚧 Not yet implemented`)

// TODO: Initialize McpServer from @modelcontextprotocol/sdk
// TODO: Register tools, resources, prompts
// TODO: Start transport (stdio / SSE)
