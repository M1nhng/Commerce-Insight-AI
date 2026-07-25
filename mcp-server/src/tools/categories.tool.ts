/**
 * MCP Tool: categories
 *
 * Category tools: list, get tree, and get details from the product category catalog.
 *
 * Exposed tools: list_categories, get_category_tree, get_category_details
 *
 * RULE: This tool communicates ONLY via apiClient → Spring Boot REST API.
 *       No direct database access.
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';

export class CategoryToolsProvider implements McpProvider {
  public register(server: McpServer, _apiClient: AxiosInstance): void {
    
    // 1. list_categories
    server.tool(
      'list_categories',
      'Retrieve a flat list of product categories, optionally filtered by parent category ID or active status.',
      {
        parentId: z.string().uuid().optional().describe('Filter categories by a specific Parent ID'),
        active: z.boolean().optional().describe('Filter by active status'),
        page: z.number().int().min(0).optional().default(0).describe('Page number (0-indexed)'),
        size: z.number().int().min(1).max(100).optional().default(100).describe('Number of categories per page')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/categories
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for list_categories' })
            }
          ]
        };
      }
    );

    // 2. get_category_tree
    server.tool(
      'get_category_tree',
      'Retrieve the full hierarchical tree structure of all active product categories including their nested subcategories and product counts.',
      {},
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/categories/tree
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for get_category_tree' })
            }
          ]
        };
      }
    );

    // 3. get_category_details
    server.tool(
      'get_category_details',
      'Retrieve comprehensive details for a specific category by its unique ID.',
      {
        categoryId: z.string().uuid().describe('The unique UUID of the category to retrieve')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/categories/{categoryId}
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for get_category_details' })
            }
          ]
        };
      }
    );
  }
}
