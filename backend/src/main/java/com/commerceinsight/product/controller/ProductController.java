package com.commerceinsight.product.controller;

import com.commerceinsight.product.dto.request.CreateProductRequest;
import com.commerceinsight.product.dto.request.UpdateProductRequest;
import com.commerceinsight.product.dto.response.ProductResponse;
import com.commerceinsight.product.dto.response.ProductSummaryResponse;
import com.commerceinsight.product.service.ProductService;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ProductController — REST endpoints for the product domain.
 *
 * <p>Base path: {@code /api/v1/products}
 *
 * <p>Architecture Rule: Thin HTTP adapter. All business logic in {@link ProductService}.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog — CRUD, search, filtering, pagination")
public class ProductController {

    private final ProductService productService;

    // ── GET /api/v1/products ────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List products (paginated, filterable)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryResponse>>> findAll(
            @Parameter(description = "Full-text search on name or SKU")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by category ID")
            @RequestParam(required = false) UUID categoryId,

            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Minimum price (inclusive)")
            @RequestParam(required = false) BigDecimal priceMin,

            @Parameter(description = "Maximum price (inclusive)")
            @RequestParam(required = false) BigDecimal priceMax,

            @RequestParam(defaultValue = "0")             int page,
            @RequestParam(defaultValue = "10")            int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortField));

        return ResponseEntity.ok(ApiResponse.success(
                productService.findAll(search, categoryId, active, priceMin, priceMax, pageable),
                "Products retrieved successfully"
        ));
    }

    // ── GET /api/v1/products/{id} ───────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get a single product by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.findById(id),
                "Product retrieved successfully"
        ));
    }

    // ── POST /api/v1/products ───────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new product")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        productService.create(request),
                        "Product created successfully"
                ));
    }

    // ── PUT /api/v1/products/{id} ───────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.update(id, request),
                "Product updated successfully"
        ));
    }

    // ── DELETE /api/v1/products/{id} ────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
