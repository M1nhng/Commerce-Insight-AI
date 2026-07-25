/**
 * MCP Tool: products
 *
 * Product tools: list, get, search, and filter products from the catalog.
 *
 * Exposed tools: search_products, get_product_details
 *
 * RULE: This tool communicates ONLY via apiClient → Spring Boot REST API.
 *       No direct database access.
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';

export class ProductToolsProvider implements McpProvider {
  public register(server: McpServer, _apiClient: AxiosInstance): void {
    
    // 1. search_products
    server.tool(
      'search_products',
      'Search for products by name or SKU, optionally filtered by category. Returns a paginated list of products matching the criteria.',
      {
        query: z.string().optional().describe('Search term for product name or SKU'),
        categoryId: z.string().uuid().optional().describe('Filter products by a specific Category ID'),
        page: z.number().int().min(0).optional().default(0).describe('Page number (0-indexed)'),
        size: z.number().int().min(1).max(100).optional().default(10).describe('Number of products per page')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/products with appropriate query params.
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for search_products' })
            }
          ]
        };
      }
    );

    // 2. get_product_details
    server.tool(
      'get_product_details',
      'Retrieve comprehensive details for a specific product by its unique ID. Returns full product information including cost price and description.',
      {
        productId: z.string().uuid().describe('The unique UUID of the product to retrieve')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/products/{productId}
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for get_product_details' })
            }
          ]
        };
      }
    );
  }
}
