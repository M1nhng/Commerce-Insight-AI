package com.commerceinsight.category.controller;

import com.commerceinsight.category.dto.request.CreateCategoryRequest;
import com.commerceinsight.category.dto.request.UpdateCategoryRequest;
import com.commerceinsight.category.dto.response.CategoryResponse;
import com.commerceinsight.category.dto.response.CategoryTreeResponse;
import com.commerceinsight.category.service.CategoryService;
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

import java.util.List;
import java.util.UUID;

/**
 * CategoryController — REST endpoints for the category domain.
 *
 * <p>Base path: {@code /api/v1/categories}
 *
 * <p>Architecture Rule: This controller is a thin HTTP adapter.
 * All business logic lives in {@link CategoryService}.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category management — tree structure with CRUD")
public class CategoryController {

    private final CategoryService categoryService;

    // ── GET /api/v1/categories ──────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all categories (flat, paginated)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> findAll(
            @Parameter(description = "Full-text search on category name")
            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sortOrder,asc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortField));
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.findAll(search, pageable),
                "Categories retrieved successfully"
        ));
    }

    // ── GET /api/v1/categories/tree ─────────────────────────────────────────

    @GetMapping("/tree")
    @Operation(summary = "Get full category tree (hierarchical)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> findTree() {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.findTree(),
                "Category tree retrieved successfully"
        ));
    }

    // ── GET /api/v1/categories/{id} ─────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get a single category by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.findById(id),
                "Category retrieved successfully"
        ));
    }

    // ── POST /api/v1/categories ─────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new category")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        categoryService.create(request),
                        "Category created successfully"
                ));
    }

    // ── PUT /api/v1/categories/{id} ─────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.update(id, request),
                "Category updated successfully"
        ));
    }

    // ── DELETE /api/v1/categories/{id} ──────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a category (fails if it has active products)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
