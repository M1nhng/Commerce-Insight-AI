/**
 * mcp-server/src/types/product.types.ts
 * 
 * TypeScript interfaces mirroring the Spring Boot backend DTOs for the 
 * Product and Category domains. Used to type API responses within the MCP server.
 */

// Generic API Envelopes
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// Category Types
export interface CategoryResponse {
  id: string;
  name: string;
  description: string | null;
  parentId: string | null;
  sortOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryTreeResponse extends CategoryResponse {
  children: CategoryTreeResponse[];
  productCount: number;
}

// Product Types
export interface ProductSummaryResponse {
  id: string;
  sku: string;
  name: string;
  categoryId: string | null;
  categoryName: string | null;
  price: number;
  stockQuantity: number;
  active: boolean;
  imageUrl: string | null;
  createdAt: string;
}

export interface ProductResponse extends ProductSummaryResponse {
  description: string | null;
  costPrice: number | null;
  updatedAt: string;
}

// Request Types (Though MCP tools typically pass parameters as URL queries for GET requests)
export interface ProductFilterParams {
  search?: string;
  categoryId?: string;
  active?: boolean;
  minPrice?: number;
  maxPrice?: number;
  minStock?: number;
  maxStock?: number;
  page?: number;
  size?: number;
  sort?: string; // format: field,direction
}
