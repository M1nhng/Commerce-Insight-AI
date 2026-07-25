package com.commerceinsight.inventory.controller;

import com.commerceinsight.inventory.dto.request.CreateWarehouseRequest;
import com.commerceinsight.inventory.dto.request.UpdateWarehouseRequest;
import com.commerceinsight.inventory.dto.response.WarehouseResponse;
import com.commerceinsight.inventory.service.WarehouseService;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * WarehouseController — REST API for warehouse management.
 *
 * <p>Base path: /api/v1/warehouses
 */
@Tag(name = "Warehouses", description = "Warehouse management endpoints")
@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    // ── GET /api/v1/warehouses ─────────────────────────────────────────────

    @Operation(summary = "List all warehouses (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<WarehouseResponse>>> findAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        PageResponse<WarehouseResponse> result =
                warehouseService.findAll(PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(result, "Warehouses retrieved successfully"));
    }

    // ── GET /api/v1/warehouses/{id} ────────────────────────────────────────

    @Operation(summary = "Get warehouse by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(warehouseService.findById(id), "Warehouse retrieved successfully"));
    }

    // ── POST /api/v1/warehouses ────────────────────────────────────────────

    @Operation(summary = "Create a new warehouse")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            @Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse response = warehouseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Warehouse created successfully"));
    }

    // ── PUT /api/v1/warehouses/{id} ────────────────────────────────────────

    @Operation(summary = "Update a warehouse")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWarehouseRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(warehouseService.update(id, request), "Warehouse updated successfully"));
    }

    // ── DELETE /api/v1/warehouses/{id} ────────────────────────────────────

    @Operation(summary = "Soft-delete a warehouse (fails if it has stock)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
