package com.commerceinsight.inventory.controller;

import com.commerceinsight.inventory.dto.request.AdjustStockRequest;
import com.commerceinsight.inventory.dto.request.TransferStockRequest;
import com.commerceinsight.inventory.dto.response.InventoryResponse;
import com.commerceinsight.inventory.dto.response.InventoryTransactionResponse;
import com.commerceinsight.inventory.service.InventoryService;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * InventoryController — REST API for inventory management.
 *
 * <p>Base path: /api/v1/inventory
 */
@Tag(name = "Inventory", description = "Inventory stock management endpoints")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ── GET /api/v1/inventory ─────────────────────────────────────────────

    @Operation(summary = "List inventory with filters (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> findAll(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStockOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "product.name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        PageResponse<InventoryResponse> result = inventoryService.findAll(
                warehouseId, productId, search, lowStockOnly,
                PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(result, "Inventory retrieved successfully"));
    }

    // ── GET /api/v1/inventory/{id} ────────────────────────────────────────

    @Operation(summary = "Get inventory record by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<InventoryResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(inventoryService.findById(id), "Inventory retrieved successfully"));
    }

    // ── GET /api/v1/inventory/product/{productId} ─────────────────────────

    @Operation(summary = "Get all inventory records for a product (across all warehouses)")
    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> findByProduct(
            @PathVariable UUID productId) {
        return ResponseEntity.ok(
                ApiResponse.success(inventoryService.findByProduct(productId),
                        "Product inventory retrieved successfully"));
    }

    // ── GET /api/v1/inventory/low-stock ───────────────────────────────────

    @Operation(summary = "Get all inventory records that are at or below their low-stock threshold")
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> findLowStock() {
        return ResponseEntity.ok(
                ApiResponse.success(inventoryService.findLowStock(),
                        "Low-stock inventory retrieved successfully"));
    }

    // ── PATCH /api/v1/inventory/{id}/adjust ──────────────────────────────

    @Operation(summary = "Immediately adjust stock quantity (no approval required — ADMIN/MANAGER only)")
    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjust(
            @PathVariable UUID id,
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(inventoryService.adjustStock(id, request),
                        "Stock adjusted successfully"));
    }

    // ── POST /api/v1/inventory/transfer ───────────────────────────────────

    @Operation(summary = "Transfer stock between warehouses atomically")
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Void>> transfer(
            @Valid @RequestBody TransferStockRequest request) {
        inventoryService.transfer(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Stock transferred successfully"));
    }

    // ── GET /api/v1/inventory/{id}/transactions ───────────────────────────

    @Operation(summary = "Get transaction history for an inventory record (paginated)")
    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponse>>> getTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<InventoryTransactionResponse> result =
                inventoryService.getTransactions(id, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Transaction history retrieved successfully"));
    }
}
