package com.commerceinsight.order.controller;

import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;
import com.commerceinsight.order.dto.request.*;
import com.commerceinsight.order.dto.response.*;
import com.commerceinsight.order.service.OrderService;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * OrderController — REST endpoints for the Order domain.
 *
 * <p>Base path: {@code /api/v1/orders}
 *
 * <p>Architecture rule: Thin HTTP adapter. All business logic in {@link OrderService}.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order lifecycle — creation, status transitions, cancellation, search")
public class OrderController {

    private final OrderService orderService;

    // ── GET /api/v1/orders ──────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List orders (paginated, filterable)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> findAll(

            @Parameter(description = "Search by order number")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filter by order status")
            @RequestParam(required = false) OrderStatus status,

            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus paymentStatus,

            @Parameter(description = "Created after (ISO 8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateFrom,

            @Parameter(description = "Created before (ISO 8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant dateTo,

            @RequestParam(defaultValue = "0")              int page,
            @RequestParam(defaultValue = "10")             int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(direction, sortParts[0]));

        return ResponseEntity.ok(ApiResponse.success(
                orderService.findAll(keyword, customerId, status, paymentStatus, dateFrom, dateTo, pageable),
                "Orders retrieved successfully"
        ));
    }

    // ── GET /api/v1/orders/{id} ─────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get full order detail by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.findById(id),
                "Order retrieved successfully"
        ));
    }

    // ── POST /api/v1/orders ─────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        orderService.createOrder(request),
                        "Order created successfully"
                ));
    }

    // ── PATCH /api/v1/orders/{id}/status ────────────────────────────────────

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (enforces state machine)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateStatus(id, request),
                "Order status updated to " + request.status()
        ));
    }

    // ── POST /api/v1/orders/{id}/cancel ─────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order (only valid from PENDING, CONFIRMED, PROCESSING)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(id, request),
                "Order cancelled successfully"
        ));
    }
}
