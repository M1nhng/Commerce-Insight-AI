/**
 * MCP Tool: inventory
 * 
 * Inventory tools: status, history, warehouse summary, and low stock warnings.
 * 
 * Exposed tools: inventory_status, inventory_history, warehouse_summary, low_stock_products
 * 
 * RULE: This tool communicates ONLY via apiClient → Spring Boot REST API.
 *       No direct database access.
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';

export class InventoryToolsProvider implements McpProvider {
  public register(server: McpServer, _apiClient: AxiosInstance): void {
    
    // 1. inventory_status
    server.tool(
      'inventory_status',
      'Retrieve current stock levels for products across warehouses. Supports filtering by warehouse and searching by product name or SKU.',
      {
        warehouseId: z.string().uuid().optional().describe('Filter by specific Warehouse ID'),
        search: z.string().optional().describe('Search term for product name or SKU'),
        page: z.number().int().min(0).optional().default(0).describe('Page number (0-indexed)'),
        size: z.number().int().min(1).max(100).optional().default(10).describe('Number of items per page')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/inventory with appropriate query params.
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for inventory_status' })
            }
          ]
        };
      }
    );

    // 2. inventory_history
    server.tool(
      'inventory_history',
      'Retrieve the historical transaction log for inventory movements. Can filter by product, warehouse, transaction type, or reference ID.',
      {
        productId: z.string().uuid().optional().describe('Filter by specific Product ID'),
        warehouseId: z.string().uuid().optional().describe('Filter by specific Warehouse ID'),
        type: z.enum([
          'PURCHASE',
          'SALE',
          'ADJUSTMENT',
          'RETURN',
          'DAMAGE',
          'TRANSFER_IN',
          'TRANSFER_OUT'
        ]).optional().describe('Filter by transaction type'),
        referenceId: z.string().uuid().optional().describe('Filter by reference ID (e.g. Order ID, Stock Adjustment ID)'),
        page: z.number().int().min(0).optional().default(0).describe('Page number (0-indexed)'),
        size: z.number().int().min(1).max(100).optional().default(10).describe('Number of logs per page')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/inventory/transactions with appropriate query params.
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for inventory_history' })
            }
          ]
        };
      }
    );

    // 3. warehouse_summary
    server.tool(
      'warehouse_summary',
      'Retrieve a summary list of all warehouses including their code, name, location, and active status.',
      {
        active: z.boolean().optional().describe('Filter by active status'),
        page: z.number().int().min(0).optional().default(0).describe('Page number (0-indexed)'),
        size: z.number().int().min(1).max(100).optional().default(20).describe('Number of warehouses per page')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/warehouses with appropriate query params.
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for warehouse_summary' })
            }
          ]
        };
      }
    );

    // 4. low_stock_products
    server.tool(
      'low_stock_products',
      'Retrieve a list of products whose quantity is at or below the low stock threshold.',
      {
        warehouseId: z.string().uuid().optional().describe('Check low stock within a specific warehouse ID'),
        customThreshold: z.number().int().nonnegative().optional().describe('Custom low stock threshold to override default'),
        page: z.number().int().min(0).optional().default(0).describe('Page number (0-indexed)'),
        size: z.number().int().min(1).max(100).optional().default(10).describe('Number of items per page')
      },
      async (_args) => {
        // TODO: Implement execution logic in a future sprint.
        // Will use apiClient to call GET /api/v1/inventory/low-stock with appropriate query params.
        return {
          content: [
            {
              type: 'text',
              text: JSON.stringify({ message: 'Execution logic not yet implemented for low_stock_products' })
            }
          ]
        };
      }
    );
  }
}
