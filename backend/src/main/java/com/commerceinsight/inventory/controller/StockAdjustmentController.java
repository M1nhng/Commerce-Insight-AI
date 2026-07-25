package com.commerceinsight.inventory.controller;

import com.commerceinsight.inventory.domain.AdjustmentStatus;
import com.commerceinsight.inventory.dto.request.RequestStockAdjustmentRequest;
import com.commerceinsight.inventory.dto.request.ReviewStockAdjustmentRequest;
import com.commerceinsight.inventory.dto.response.StockAdjustmentResponse;
import com.commerceinsight.inventory.service.StockAdjustmentService;
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
 * StockAdjustmentController — REST API for the stock adjustment approval workflow.
 *
 * <p>Base path: /api/v1/stock-adjustments
 *
 * <p>Lifecycle: POST (request) → PATCH /approve or /reject (ADMIN only)
 */
@Tag(name = "Stock Adjustments", description = "Stock adjustment approval workflow endpoints")
@RestController
@RequestMapping("/api/v1/stock-adjustments")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockAdjustmentService stockAdjustmentService;

    // ── GET /api/v1/stock-adjustments ─────────────────────────────────────

    @Operation(summary = "List stock adjustments with filters (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<StockAdjustmentResponse>>> findAll(
            @RequestParam(required = false) AdjustmentStatus status,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID requestedBy,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageResponse<StockAdjustmentResponse> result = stockAdjustmentService.findAll(
                status, warehouseId, productId, requestedBy,
                PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(result, "Stock adjustments retrieved successfully"));
    }

    // ── GET /api/v1/stock-adjustments/{id} ────────────────────────────────

    @Operation(summary = "Get stock adjustment by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(stockAdjustmentService.findById(id),
                        "Stock adjustment retrieved successfully"));
    }

    // ── POST /api/v1/stock-adjustments ────────────────────────────────────

    @Operation(summary = "Request a stock adjustment (creates PENDING record, does not modify inventory)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> request(
            @Valid @RequestBody RequestStockAdjustmentRequest request) {
        StockAdjustmentResponse response = stockAdjustmentService.request(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock adjustment requested successfully"));
    }

    // ── PATCH /api/v1/stock-adjustments/{id}/approve ─────────────────────

    @Operation(summary = "Approve a PENDING adjustment — applies stock change (ADMIN only)")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReviewStockAdjustmentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(stockAdjustmentService.approve(id, request),
                        "Stock adjustment approved and applied successfully"));
    }

    // ── PATCH /api/v1/stock-adjustments/{id}/reject ───────────────────────

    @Operation(summary = "Reject a PENDING adjustment — no stock change made (ADMIN only)")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReviewStockAdjustmentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(stockAdjustmentService.reject(id, request),
                        "Stock adjustment rejected successfully"));
    }
}
