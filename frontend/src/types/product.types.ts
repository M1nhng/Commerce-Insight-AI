/**
 * types/product.types.ts — Product & Category domain type definitions.
 * Mirrors the Spring Boot DTOs from the backend API.
 */

// ── Category Types ────────────────────────────────────────────────────────

export interface CategoryResponse {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  parentId: string | null;
  sortOrder: number;
  active: boolean;
  productCount: number;
  createdAt: string;
}

export interface CategoryTreeResponse extends CategoryResponse {
  children: CategoryTreeResponse[];
}

export interface CreateCategoryRequest {
  name: string;
  description?: string | null;
  parentId?: string | null;
  sortOrder?: number;
}

export interface UpdateCategoryRequest {
  name: string;
  description?: string | null;
  parentId?: string | null;
  sortOrder?: number;
  active?: boolean;
}

// ── Product Types ─────────────────────────────────────────────────────────

export interface ProductSummaryResponse {
  id: string;
  sku: string;
  name: string;
  price: number;
  categoryId: string | null;
  categoryName: string | null;
  stockQuantity: number;
  active: boolean;
  imageUrl: string | null;
  createdAt: string;
}

export interface ProductImageResponse {
  id: string;
  url: string;
  altText: string | null;
  sortOrder: number;
}

export interface ProductResponse {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  price: number;
  costPrice: number | null;
  imageUrl: string | null;
  categoryId: string | null;
  categoryName: string | null;
  active: boolean;
  stockQuantity: number;
  images: ProductImageResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  sku: string;
  name: string;
  description?: string | null;
  price: number;
  costPrice?: number | null;
  imageUrl?: string | null;
  categoryId?: string | null;
  initialStock?: number;
}

export interface UpdateProductRequest {
  sku: string;
  name: string;
  description?: string | null;
  price: number;
  costPrice?: number | null;
  imageUrl?: string | null;
  categoryId?: string | null;
  active?: boolean;
}

// ── Filter Types ──────────────────────────────────────────────────────────

export interface ProductFilterParams {
  search?: string;
  categoryId?: string;
  active?: boolean;
  priceMin?: number;
  priceMax?: number;
  page?: number;
  size?: number;
  sort?: string;
}
