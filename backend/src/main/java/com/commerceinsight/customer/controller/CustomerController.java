package com.commerceinsight.customer.controller;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.dto.request.*;
import com.commerceinsight.customer.dto.response.CustomerAddressResponse;
import com.commerceinsight.customer.dto.response.CustomerResponse;
import com.commerceinsight.customer.dto.response.CustomerSummaryResponse;
import com.commerceinsight.customer.service.CustomerAddressService;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.List;
import java.util.UUID;

/**
 * CustomerController — REST API for customer management.
 *
 * <p>Base path: /api/v1/customers
 */
@Tag(name = "Customers", description = "Customer management endpoints")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerAddressService addressService;

    // ── GET /api/v1/customers ─────────────────────────────────────────────────

    @Operation(summary = "List customers with search, filter and pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerSummaryResponse>>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageResponse<CustomerSummaryResponse> result = customerService.findAll(
                keyword, status, groupId, startDate, endDate,
                PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success(result, "Customers retrieved successfully"));
    }

    // ── GET /api/v1/customers/{id} ────────────────────────────────────────────

    @Operation(summary = "Get customer by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(customerService.findById(id), "Customer retrieved successfully"));
    }

    // ── POST /api/v1/customers ────────────────────────────────────────────────

    @Operation(summary = "Create a new customer")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer created successfully"));
    }

    // ── PUT /api/v1/customers/{id} ────────────────────────────────────────────

    @Operation(summary = "Update an existing customer")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(customerService.update(id, request), "Customer updated successfully"));
    }

    // ── DELETE /api/v1/customers/{id} ─────────────────────────────────────────

    @Operation(summary = "Soft-delete a customer")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── PATCH /api/v1/customers/{id}/status ──────────────────────────────────

    @Operation(summary = "Update customer status (ACTIVE, INACTIVE, BLOCKED)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(customerService.updateStatus(id, request), "Customer status updated successfully"));
    }

    // ── Address sub-resource ──────────────────────────────────────────────────

    @Operation(summary = "Get all addresses for a customer")
    @GetMapping("/{id}/addresses")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getAddresses(@PathVariable UUID id) {
        customerService.getOrThrow(id); // validates customer exists
        return ResponseEntity.ok(
                ApiResponse.success(addressService.findAllByCustomer(id), "Addresses retrieved successfully"));
    }

    @Operation(summary = "Add an address to a customer")
    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> addAddress(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAddressRequest request) {
        Customer customer = customerService.getOrThrow(id);
        CustomerAddressResponse response = addressService.addAddress(customer, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Address added successfully"));
    }

    @Operation(summary = "Update a customer address")
    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> updateAddress(
            @PathVariable UUID id,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        addressService.updateAddress(id, addressId, request),
                        "Address updated successfully"));
    }

    @Operation(summary = "Delete a customer address")
    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID id,
            @PathVariable UUID addressId) {
        addressService.deleteAddress(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Set an address as the default for its type")
    @PatchMapping("/{id}/addresses/{addressId}/default")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> setDefault(
            @PathVariable UUID id,
            @PathVariable UUID addressId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        addressService.setDefault(id, addressId),
                        "Default address updated successfully"));
    }
}
