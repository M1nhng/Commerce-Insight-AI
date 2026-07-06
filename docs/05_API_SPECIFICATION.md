# 05 — API Specification
# Commerce Insight AI

> **Document Type**: API Design
> **Version**: 1.0.0
> **Status**: Approved
> **Last Updated**: 2026-07-06
> **Owner**: Chief Solution Architect

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [REST Standards](#2-rest-standards)
3. [Response Format](#3-response-format)
4. [Error Format](#4-error-format)
5. [Pagination, Filtering & Sorting](#5-pagination-filtering--sorting)
6. [Versioning](#6-versioning)
7. [Authentication APIs](#7-authentication-apis)
8. [User APIs](#8-user-apis)
9. [Product APIs](#9-product-apis)
10. [Category APIs](#10-category-apis)
11. [Customer APIs](#11-customer-apis)
12. [Order APIs](#12-order-apis)
13. [Inventory APIs](#13-inventory-apis)
14. [Analytics APIs](#14-analytics-apis)
15. [Import APIs](#15-import-apis)
16. [Export APIs](#16-export-apis)
17. [AI APIs](#17-ai-apis)
18. [Admin APIs](#18-admin-apis)

---

## 1. Purpose

This document defines the complete REST API specification for the Commerce Insight AI Spring Boot backend. It defines:
- HTTP methods, endpoints, request/response contracts
- Standard response and error envelope formats
- Pagination, filtering, and sorting conventions
- API versioning strategy

All controllers MUST implement the endpoints exactly as specified here.

---

## 2. REST Standards

### 2.1 HTTP Methods

| Method | Usage |
|--------|-------|
| `GET` | Retrieve resources (idempotent, no body) |
| `POST` | Create a new resource |
| `PUT` | Full replacement update of a resource |
| `PATCH` | Partial update of a resource |
| `DELETE` | Remove a resource (soft delete where applicable) |

### 2.2 URL Conventions

| Rule | Example |
|------|---------|
| Lowercase, hyphen-separated | `/order-items` not `/orderItems` |
| Plural nouns for collections | `/products`, `/orders` |
| Singular noun for sub-resources | `/orders/{id}/items` |
| No trailing slash | `/products` not `/products/` |
| Version prefix | `/api/v1/products` |

### 2.3 HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | GET, PUT, PATCH success |
| 201 | Created | POST success (new resource created) |
| 204 | No Content | DELETE success, or actions with no response body |
| 400 | Bad Request | Validation error, malformed request |
| 401 | Unauthorized | Missing or invalid JWT |
| 403 | Forbidden | JWT valid but insufficient role |
| 404 | Not Found | Resource with given ID does not exist |
| 409 | Conflict | Duplicate resource (e.g., duplicate SKU) |
| 422 | Unprocessable Entity | Business rule violation |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected server error |
| 503 | Service Unavailable | LLM provider unavailable |

---

## 3. Response Format

### 3.1 Success Response Envelope

All successful API responses wrap data in `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { },
  "message": "Products retrieved successfully",
  "timestamp": "2026-07-06T10:00:00.000Z"
}
```

### 3.2 Paginated Response

For list endpoints, `data` contains a `PageResponse<T>`:

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 248,
    "totalPages": 25,
    "first": true,
    "last": false
  },
  "message": "Products retrieved successfully",
  "timestamp": "2026-07-06T10:00:00.000Z"
}
```

---

## 4. Error Format

### 4.1 Single Error

```json
{
  "success": false,
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "Product with ID '550e8400-e29b-41d4-a716-446655440000' was not found",
    "details": null
  },
  "timestamp": "2026-07-06T10:00:00.000Z"
}
```

### 4.2 Validation Error (Multiple Fields)

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      { "field": "name", "message": "must not be blank" },
      { "field": "price", "message": "must be greater than 0" }
    ]
  },
  "timestamp": "2026-07-06T10:00:00.000Z"
}
```

### 4.3 Error Codes Reference

| Code | HTTP Status | Meaning |
|------|-------------|---------|
| `VALIDATION_ERROR` | 400 | Bean validation failed |
| `AUTHENTICATION_REQUIRED` | 401 | No valid JWT |
| `TOKEN_EXPIRED` | 401 | JWT has expired |
| `REFRESH_TOKEN_INVALID` | 401 | Invalid or revoked refresh token |
| `ACCESS_DENIED` | 403 | Insufficient role |
| `RESOURCE_NOT_FOUND` | 404 | Generic not found |
| `USER_NOT_FOUND` | 404 | User not found |
| `PRODUCT_NOT_FOUND` | 404 | Product not found |
| `ORDER_NOT_FOUND` | 404 | Order not found |
| `CUSTOMER_NOT_FOUND` | 404 | Customer not found |
| `DUPLICATE_SKU` | 409 | SKU already exists |
| `DUPLICATE_EMAIL` | 409 | Email already registered |
| `INVALID_STATUS_TRANSITION` | 422 | Invalid order status change |
| `INSUFFICIENT_STOCK` | 422 | Stock below required quantity |
| `CATEGORY_HAS_PRODUCTS` | 422 | Cannot delete category with products |
| `IMPORT_VALIDATION_FAILED` | 422 | Import file has validation errors |
| `AI_PROVIDER_UNAVAILABLE` | 503 | LLM provider is not responding |

---

## 5. Pagination, Filtering & Sorting

### 5.1 Pagination Query Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-indexed) |
| `size` | integer | 10 | Items per page (max: 100) |

### 5.2 Sorting Query Parameters

| Parameter | Format | Example |
|-----------|--------|---------|
| `sort` | `{field},{direction}` | `sort=createdAt,desc` |
| Multiple sorts | Multiple `sort` params | `sort=name,asc&sort=price,desc` |

### 5.3 Common Filter Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `search` | string | Full-text search on primary fields |
| `status` | string | Filter by status enum |
| `categoryId` | UUID | Filter by category |
| `startDate` | ISO 8601 date | From date (inclusive) |
| `endDate` | ISO 8601 date | To date (inclusive) |
| `active` | boolean | Filter by active status |

---

## 6. Versioning

### 6.1 Strategy

**URL versioning** is used: `/api/v1/...`

All endpoints are prefixed with `/api/v1`. Future breaking changes introduce `/api/v2/...` while maintaining v1 for backward compatibility.

### 6.2 Version Header

Clients may also send `Accept: application/vnd.cia.v1+json` for version negotiation (future).

---

## 7. Authentication APIs

Base path: `/api/v1/auth`

### POST `/api/v1/auth/register`

**Permission**: Public

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "550e8400-...",
    "expiresIn": 900,
    "user": {
      "id": "...",
      "email": "john.doe@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "role": "STAFF"
    }
  }
}
```

---

### POST `/api/v1/auth/login`

**Permission**: Public

**Request Body:**
```json
{ "email": "john.doe@example.com", "password": "SecurePass123!" }
```

**Response 200:** Same as register response.

**Response 401:** `AUTHENTICATION_REQUIRED` — Invalid credentials.

---

### POST `/api/v1/auth/refresh`

**Permission**: Public (uses refresh token, not access token)

**Request Body:**
```json
{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "new-refresh-token-uuid",
    "expiresIn": 900
  }
}
```

**Response 401:** `REFRESH_TOKEN_INVALID`

---

### POST `/api/v1/auth/logout`

**Permission**: Authenticated

**Request:** No body. JWT in Authorization header identifies the user.

**Response 204:** All refresh tokens for the user are revoked.

---

### GET `/api/v1/auth/me`

**Permission**: Authenticated

**Response 200:** Returns current user profile.

---

## 8. User APIs

Base path: `/api/v1/users`

### GET `/api/v1/users` — ADMIN only
### GET `/api/v1/users/{id}` — ADMIN only
### POST `/api/v1/users` — ADMIN only
### PUT `/api/v1/users/{id}` — ADMIN only
### PATCH `/api/v1/users/{id}/role` — ADMIN only
### DELETE `/api/v1/users/{id}` — ADMIN only (soft delete)

**UserResponse DTO:**
```json
{
  "id": "uuid",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "role": "ADMIN|MANAGER|STAFF",
  "active": true,
  "locked": false,
  "lastLoginAt": "ISO 8601",
  "createdAt": "ISO 8601"
}
```

---

## 9. Product APIs

Base path: `/api/v1/products`

### GET `/api/v1/products`

**Permission**: ADMIN, MANAGER, STAFF

**Query Params:** `page`, `size`, `sort`, `search`, `categoryId`, `active`

**Response 200:** `PageResponse<ProductSummaryResponse>`

**ProductSummaryResponse:**
```json
{
  "id": "uuid",
  "sku": "SKU-001",
  "name": "Wireless Headphones Pro",
  "price": 49.99,
  "categoryId": "uuid",
  "categoryName": "Electronics",
  "stockQuantity": 124,
  "active": true,
  "imageUrl": "https://...",
  "createdAt": "ISO 8601"
}
```

---

### GET `/api/v1/products/{id}`

**Permission**: ADMIN, MANAGER, STAFF

**Response 200:** Full `ProductResponse` with all fields.

---

### POST `/api/v1/products`

**Permission**: ADMIN, MANAGER

**Request Body:**
```json
{
  "sku": "SKU-001",
  "name": "Wireless Headphones Pro",
  "description": "Premium noise-cancelling headphones",
  "price": 49.99,
  "costPrice": 22.00,
  "categoryId": "uuid",
  "imageUrl": "https://...",
  "initialStock": 100
}
```

**Response 201:** `ProductResponse`

**Response 409:** `DUPLICATE_SKU`

---

### PUT `/api/v1/products/{id}`

**Permission**: ADMIN, MANAGER

**Response 200:** Updated `ProductResponse`

---

### DELETE `/api/v1/products/{id}`

**Permission**: ADMIN

**Response 204:** Soft deleted.

---

## 10. Category APIs

Base path: `/api/v1/categories`

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/categories` | All | List all categories (flat or tree) |
| GET | `/api/v1/categories/tree` | All | Hierarchical tree structure |
| GET | `/api/v1/categories/{id}` | All | Single category detail |
| POST | `/api/v1/categories` | ADMIN, MANAGER | Create category |
| PUT | `/api/v1/categories/{id}` | ADMIN, MANAGER | Update category |
| DELETE | `/api/v1/categories/{id}` | ADMIN | Soft delete (fails if has products) |

**CategoryResponse:**
```json
{
  "id": "uuid",
  "name": "Electronics",
  "slug": "electronics",
  "description": "string",
  "parentId": null,
  "sortOrder": 0,
  "productCount": 42,
  "children": []
}
```

---

## 11. Customer APIs

Base path: `/api/v1/customers`

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/customers` | ADMIN, MANAGER | Paginated list |
| GET | `/api/v1/customers/{id}` | ADMIN, MANAGER | Customer detail |
| GET | `/api/v1/customers/{id}/orders` | ADMIN, MANAGER | Customer order history |
| POST | `/api/v1/customers` | ADMIN, MANAGER | Create customer |
| PUT | `/api/v1/customers/{id}` | ADMIN, MANAGER | Update customer |
| DELETE | `/api/v1/customers/{id}` | ADMIN | Soft delete |

**CustomerResponse:**
```json
{
  "id": "uuid",
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@example.com",
  "phone": "+1-555-0123",
  "city": "New York",
  "country": "US",
  "totalOrders": 12,
  "totalSpent": 1240.50,
  "createdAt": "ISO 8601"
}
```

---

## 12. Order APIs

Base path: `/api/v1/orders`

### GET `/api/v1/orders`

**Permission**: ADMIN, MANAGER, STAFF

**Query Params:** `page`, `size`, `sort`, `status`, `customerId`, `startDate`, `endDate`, `search`

**Response 200:** `PageResponse<OrderSummaryResponse>`

---

### GET `/api/v1/orders/{id}`

**Response 200:** Full `OrderResponse` with `lineItems`.

**OrderResponse:**
```json
{
  "id": "uuid",
  "orderNumber": "ORD-2026-001",
  "customerId": "uuid",
  "customerName": "Jane Smith",
  "status": "SHIPPED",
  "subtotal": 159.98,
  "discount": 10.00,
  "shippingFee": 5.99,
  "tax": 12.50,
  "total": 168.47,
  "lineItems": [
    {
      "id": "uuid",
      "productId": "uuid",
      "productName": "Wireless Headphones Pro",
      "productSku": "SKU-001",
      "quantity": 2,
      "unitPrice": 49.99,
      "discount": 0,
      "total": 99.98
    }
  ],
  "notes": "Handle with care",
  "createdAt": "ISO 8601",
  "shippedAt": "ISO 8601"
}
```

---

### POST `/api/v1/orders`

**Permission**: ADMIN, MANAGER

**Request Body:**
```json
{
  "customerId": "uuid",
  "lineItems": [
    { "productId": "uuid", "quantity": 2, "unitPrice": 49.99, "discount": 0 }
  ],
  "shippingFee": 5.99,
  "discount": 10.00,
  "tax": 12.50,
  "notes": "Handle with care"
}
```

**Response 201:** `OrderResponse`

**Response 422:** `INSUFFICIENT_STOCK`

---

### PATCH `/api/v1/orders/{id}/status`

**Permission**: ADMIN, MANAGER (STAFF can only update specific transitions)

**Request Body:**
```json
{ "status": "SHIPPED", "note": "Shipped via FedEx tracking #XYZ123" }
```

**Response 200:** Updated `OrderResponse`

**Response 422:** `INVALID_STATUS_TRANSITION`

---

## 13. Inventory APIs

Base path: `/api/v1/inventory`

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/api/v1/inventory` | ADMIN, MANAGER, STAFF | Paginated inventory list |
| GET | `/api/v1/inventory/{productId}` | All | Stock for a product |
| GET | `/api/v1/inventory/low-stock` | ADMIN, MANAGER | Products below threshold |
| PATCH | `/api/v1/inventory/{productId}` | ADMIN, MANAGER | Manual stock adjustment |
| GET | `/api/v1/inventory/{productId}/movements` | ADMIN, MANAGER | Movement history |

**InventoryResponse:**
```json
{
  "productId": "uuid",
  "productName": "Wireless Headphones Pro",
  "productSku": "SKU-001",
  "quantity": 124,
  "reservedQuantity": 8,
  "availableQuantity": 116,
  "lowStockThreshold": 10,
  "isLowStock": false,
  "updatedAt": "ISO 8601"
}
```

---

## 14. Analytics APIs

Base path: `/api/v1/analytics`

### GET `/api/v1/analytics/dashboard`

**Permission**: ADMIN, MANAGER, STAFF

**Query Params:** `period=month` (day|week|month|quarter|year)

**Response:**
```json
{
  "period": "month",
  "revenue": { "total": 124580.50, "change": 12.4, "changeType": "INCREASE" },
  "orders": { "total": 1842, "change": 8.2, "changeType": "INCREASE" },
  "customers": { "total": 367, "new": 42, "change": 5.1 },
  "products": { "total": 248, "active": 230 }
}
```

---

### GET `/api/v1/analytics/revenue/summary`

**Query Params:** `period`, `startDate`, `endDate`

**Response:**
```json
{
  "period": "month",
  "totalRevenue": 124580.50,
  "totalOrders": 1842,
  "averageOrderValue": 67.64,
  "previousPeriodRevenue": 110870.00,
  "revenueChange": 12.4
}
```

---

### GET `/api/v1/analytics/revenue/trend`

**Query Params:** `period`, `granularity=day|week|month`

**Response:** Array of `{ date, revenue, orderCount }`

---

### GET `/api/v1/analytics/revenue/by-category`

**Query Params:** `period`

**Response:** Array of `{ categoryId, categoryName, revenue, percentage }`

---

### GET `/api/v1/analytics/products/top`

**Query Params:** `limit=10`, `period`, `metric=revenue|units`

**Response:** Array of `{ productId, productName, sku, revenue, unitsSold }`

---

### GET `/api/v1/analytics/orders/summary`

**Query Params:** `period`

**Response:**
```json
{
  "totalOrders": 1842,
  "byStatus": {
    "PENDING": 45, "CONFIRMED": 120, "PROCESSING": 98,
    "SHIPPED": 320, "DELIVERED": 1200, "CANCELLED": 42, "REFUNDED": 17
  },
  "fulfillmentRate": 95.2,
  "cancellationRate": 2.3,
  "averageFulfillmentDays": 3.2
}
```

---

## 15. Import APIs

Base path: `/api/v1/import`

### POST `/api/v1/import/products`

**Permission**: ADMIN, MANAGER

**Request:** `multipart/form-data` with file field `file`

**Supported formats:** `.csv`, `.xlsx`

**Max file size:** 10MB

**Response 200:**
```json
{
  "success": true,
  "data": {
    "jobId": "uuid",
    "type": "PRODUCTS",
    "successCount": 98,
    "errorCount": 2,
    "errors": [
      { "row": 15, "field": "price", "message": "must be greater than 0" },
      { "row": 42, "field": "sku", "message": "SKU 'SKU-042' already exists" }
    ]
  }
}
```

### POST `/api/v1/import/customers`
### POST `/api/v1/import/orders`

Same structure as product import.

---

### GET `/api/v1/import/template/{type}`

**Permission**: ADMIN, MANAGER

**Path Variable:** `type` = `products|customers|orders`

**Response:** Download the CSV template file for the given type.

---

## 16. Export APIs

Base path: `/api/v1/export`

### GET `/api/v1/export/orders`

**Permission**: ADMIN, MANAGER

**Query Params:** `format=pdf|excel`, `status`, `startDate`, `endDate`

**Response:** Binary file download with appropriate Content-Type.

### GET `/api/v1/export/products`

**Query Params:** `format=excel`, `categoryId`, `active`

### GET `/api/v1/export/analytics`

**Query Params:** `format=pdf|excel`, `period`, `startDate`, `endDate`

---

## 17. AI APIs

Base path: `/api/v1/ai`

### POST `/api/v1/ai/chat`

**Permission**: ADMIN, MANAGER, STAFF

**Request Body:**
```json
{
  "sessionId": "uuid or null (creates new session)",
  "message": "What were my top 5 products this month by revenue?"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "sessionId": "uuid",
    "reply": "Your top 5 products by revenue this month were:\n\n1. **Wireless Headphones Pro** — $12,400\n...",
    "toolsUsed": ["get_top_products"],
    "tokenUsage": { "prompt": 450, "completion": 210 },
    "provider": "openai"
  }
}
```

---

### GET `/api/v1/ai/sessions`

**Permission**: Authenticated (returns only the calling user's sessions)

**Response 200:** `PageResponse<ConversationSessionResponse>`

---

### GET `/api/v1/ai/sessions/{id}/messages`

**Permission**: Authenticated (own sessions only)

**Response 200:** `List<ConversationMessageResponse>`

---

### DELETE `/api/v1/ai/sessions/{id}`

**Permission**: Authenticated (own sessions only)

**Response 204:** Session archived.

---

## 18. Admin APIs

Base path: `/api/v1/admin`

All admin endpoints require **ADMIN** role.

### GET `/api/v1/admin/audit-logs`

**Query Params:** `page`, `size`, `userId`, `action`, `entityType`, `startDate`, `endDate`

### GET `/api/v1/admin/settings`

Returns all system settings as `List<SystemSettingResponse>`.

### PUT `/api/v1/admin/settings/{key}`

**Request Body:** `{ "value": "claude" }`

Updates a single setting key.
