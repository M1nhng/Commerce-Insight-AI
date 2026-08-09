/**
 * MCP Tool: customer
 *
 * Customer domain tools for the Commerce Insight AI MCP Server.
 *
 * Exposed tools:
 *   - customer_lookup        → GET /api/v1/customers/{id}        (fast ID-based fetch)
 *   - customer_search        → GET /api/v1/customers             (filtered + paginated list)
 *   - customer_profile       → GET /api/v1/customers/{id}        (full profile with addresses)
 *   - customer_group_summary → GET /api/v1/customer-groups/{id}  (single group detail)
 *
 * RULE: This tool communicates ONLY via apiClient → Spring Boot REST API.
 *       No direct database access.
 *       No LLM calls.
 *       No AI segmentation.
 *       No RFM logic.
 *
 * Output contract: all tools return { content: [{ type: 'text', text: JSON.stringify(data) }] }
 * Error contract:  all errors return { content: [{ type: 'text', text: '<human-readable message>' }], isError: true }
 */

import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { AxiosInstance, AxiosError } from 'axios';
import { McpProvider } from '../providers/provider.interface.js';

// ── Shared error formatter ────────────────────────────────────────────────────

/**
 * Converts an Axios error (or unknown error) into a human-readable MCP error
 * response that an LLM can communicate to the user.
 *
 * Never exposes stack traces, DB connection info, or internal system details.
 */
function toMcpError(toolName: string, error: unknown): { content: { type: 'text'; text: string }[]; isError: true } {
  if (error instanceof AxiosError) {
    const status  = error.response?.status;
    const apiMsg  = (error.response?.data as any)?.error?.message
                 ?? (error.response?.data as any)?.message
                 ?? error.message;

    if (!error.response) {
      return {
        content: [{ type: 'text', text: `Error [${toolName}]: Unable to reach the Commerce Insight backend. Please ensure the API server is running.` }],
        isError: true,
      };
    }

    if (status === 400) {
      return {
        content: [{ type: 'text', text: `Error [${toolName}]: Invalid request — ${apiMsg}` }],
        isError: true,
      };
    }

    if (status === 401 || status === 403) {
      return {
        content: [{ type: 'text', text: `Error [${toolName}]: Access denied. The MCP service key may be invalid or expired.` }],
        isError: true,
      };
    }

    if (status === 404) {
      return {
        content: [{ type: 'text', text: `Error [${toolName}]: The requested resource was not found. ${apiMsg}` }],
        isError: true,
      };
    }

    if (status === 429) {
      return {
        content: [{ type: 'text', text: `Error [${toolName}]: Rate limit exceeded. Please wait a moment before retrying.` }],
        isError: true,
      };
    }

    if (status && status >= 500) {
      return {
        content: [{ type: 'text', text: `Error [${toolName}]: The Commerce Insight backend encountered an internal error (HTTP ${status}). Please try again shortly.` }],
        isError: true,
      };
    }

    return {
      content: [{ type: 'text', text: `Error [${toolName}]: Unexpected API error (HTTP ${status ?? 'unknown'}). ${apiMsg}` }],
      isError: true,
    };
  }

  return {
    content: [{ type: 'text', text: `Error [${toolName}]: An unexpected error occurred. Please try again.` }],
    isError: true,
  };
}

// ── CustomerToolsProvider ─────────────────────────────────────────────────────

export class CustomerToolsProvider implements McpProvider {
  public register(server: McpServer, apiClient: AxiosInstance): void {

    // ──────────────────────────────────────────────────────────────────────────
    // 1. customer_lookup
    //    Fast single-customer fetch by ID.
    //    Returns summary fields: code, name, email, phone, status, group, dates.
    //    Use this when you already have the customer ID and need a quick look-up.
    //    Backend endpoint: GET /api/v1/customers/{customerId}
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'customer_lookup',
      'Look up a specific customer by their unique ID. Returns the customer code, full name, email, phone number, current status (ACTIVE, INACTIVE, or BLOCKED), assigned customer group, and account creation date. Use this when you already know the customer ID.',
      {
        customerId: z
          .string()
          .uuid('customerId must be a valid UUID')
          .describe('The unique UUID identifier of the customer to look up'),
      },
      async ({ customerId }) => {
        try {
          const response = await apiClient.get(`/customers/${customerId}`);
          const customer = response.data?.data;

          if (!customer) {
            return {
              content: [{ type: 'text', text: `No customer data returned for ID: ${customerId}` }],
              isError: true,
            };
          }

          // Project only the summary fields — exclude addresses for speed
          const summary = {
            id:           customer.id,
            customerCode: customer.customerCode,
            fullName:     customer.fullName,
            firstName:    customer.firstName,
            lastName:     customer.lastName,
            email:        customer.email    ?? null,
            phone:        customer.phone    ?? null,
            status:       customer.status,
            groupId:      customer.groupId  ?? null,
            groupName:    customer.groupName ?? null,
            createdAt:    customer.createdAt,
            updatedAt:    customer.updatedAt,
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(summary, null, 2) }],
          };
        } catch (error) {
          return toMcpError('customer_lookup', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 2. customer_search
    //    Paginated, filtered search across the full customer list.
    //    Supports text search (name, email, phone, code), status filter,
    //    group filter, and pagination controls.
    //    Backend endpoint: GET /api/v1/customers
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'customer_search',
      'Search and filter the full customer list. Supports free-text search across customer name, email, phone number, and customer code. Optionally filter by account status (ACTIVE, INACTIVE, BLOCKED) or by customer group ID. Returns a paginated list of customer summaries.',
      {
        keyword: z
          .string()
          .optional()
          .describe('Free-text search term — matches against customer name, email, phone, or code'),

        status: z
          .enum(['ACTIVE', 'INACTIVE', 'BLOCKED'])
          .optional()
          .describe('Filter by customer account status: ACTIVE, INACTIVE, or BLOCKED'),

        group: z
          .string()
          .uuid('group must be a valid UUID if provided')
          .optional()
          .describe('Filter by customer group UUID — returns only customers in this group'),

        page: z
          .number()
          .int()
          .min(0)
          .optional()
          .default(0)
          .describe('Page number for pagination, 0-indexed (default: 0)'),

        size: z
          .number()
          .int()
          .min(1)
          .max(100)
          .optional()
          .default(10)
          .describe('Number of customers to return per page (default: 10, max: 100)'),
      },
      async ({ keyword, status, group, page, size }) => {
        try {
          const params: Record<string, unknown> = {
            page,
            size,
            sortBy:  'createdAt',
            sortDir: 'desc',
          };

          if (keyword) params['keyword'] = keyword;
          if (status)  params['status']  = status;
          if (group)   params['groupId'] = group;

          const response = await apiClient.get('/customers', { params });
          const pageData = response.data?.data;

          if (!pageData) {
            return {
              content: [{ type: 'text', text: 'No customer data returned from the backend.' }],
              isError: true,
            };
          }

          const result = {
            totalElements: pageData.totalElements,
            totalPages:    pageData.totalPages,
            currentPage:   pageData.number,
            pageSize:      pageData.size,
            customers:     (pageData.content ?? []).map((c: any) => ({
              id:           c.id,
              customerCode: c.customerCode,
              fullName:     c.fullName,
              email:        c.email    ?? null,
              phone:        c.phone    ?? null,
              status:       c.status,
              groupName:    c.groupName ?? null,
              createdAt:    c.createdAt,
            })),
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpError('customer_search', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 3. customer_profile
    //    Full customer detail including all addresses.
    //    Richer than customer_lookup — use when full address data is required.
    //    Backend endpoint: GET /api/v1/customers/{customerId}
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'customer_profile',
      'Retrieve the complete profile for a specific customer, including personal details (name, email, phone, date of birth, gender), account status, customer group assignment, and all registered shipping and billing addresses. Use this when you need the full customer record, not just a summary.',
      {
        customerId: z
          .string()
          .uuid('customerId must be a valid UUID')
          .describe('The unique UUID identifier of the customer'),
      },
      async ({ customerId }) => {
        try {
          const response = await apiClient.get(`/customers/${customerId}`);
          const customer = response.data?.data;

          if (!customer) {
            return {
              content: [{ type: 'text', text: `No customer profile found for ID: ${customerId}` }],
              isError: true,
            };
          }

          // Full profile — include all fields + addresses
          const profile = {
            id:           customer.id,
            customerCode: customer.customerCode,
            fullName:     customer.fullName,
            firstName:    customer.firstName,
            lastName:     customer.lastName,
            email:        customer.email       ?? null,
            phone:        customer.phone       ?? null,
            dateOfBirth:  customer.dateOfBirth ?? null,
            gender:       customer.gender      ?? null,
            status:       customer.status,
            groupId:      customer.groupId     ?? null,
            groupName:    customer.groupName   ?? null,
            addresses:    (customer.addresses ?? []).map((addr: any) => ({
              id:            addr.id,
              type:          addr.type,
              recipientName: addr.recipientName,
              phone:         addr.phone       ?? null,
              addressLine:   addr.addressLine,
              ward:          addr.ward        ?? null,
              district:      addr.district    ?? null,
              province:      addr.province    ?? null,
              country:       addr.country,
              isDefault:     addr.isDefault,
            })),
            createdAt: customer.createdAt,
            updatedAt: customer.updatedAt,
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(profile, null, 2) }],
          };
        } catch (error) {
          return toMcpError('customer_profile', error);
        }
      }
    );

    // ──────────────────────────────────────────────────────────────────────────
    // 4. customer_group_summary
    //    Retrieve details about a specific customer group.
    //    Returns code, name, description, and active status.
    //    Backend endpoint: GET /api/v1/customer-groups/{groupId}
    // ──────────────────────────────────────────────────────────────────────────
    server.tool(
      'customer_group_summary',
      'Retrieve detailed information about a specific customer group by its unique ID. Returns the group code, display name, description, current status (ACTIVE or INACTIVE), and timestamps. Use this to understand what a customer group represents before filtering customers by group.',
      {
        groupId: z
          .string()
          .uuid('groupId must be a valid UUID')
          .describe('The unique UUID identifier of the customer group'),
      },
      async ({ groupId }) => {
        try {
          const response = await apiClient.get(`/customer-groups/${groupId}`);
          const group = response.data?.data;

          if (!group) {
            return {
              content: [{ type: 'text', text: `No customer group found for ID: ${groupId}` }],
              isError: true,
            };
          }

          const result = {
            id:          group.id,
            code:        group.code,
            name:        group.name,
            description: group.description ?? null,
            status:      group.status,
            createdAt:   group.createdAt,
            updatedAt:   group.updatedAt,
          };

          return {
            content: [{ type: 'text', text: JSON.stringify(result, null, 2) }],
          };
        } catch (error) {
          return toMcpError('customer_group_summary', error);
        }
      }
    );
  }
}
