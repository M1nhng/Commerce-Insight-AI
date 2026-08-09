package com.commerceinsight.customer.controller;

import com.commerceinsight.customer.dto.request.CreateCustomerGroupRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerGroupRequest;
import com.commerceinsight.customer.dto.response.CustomerGroupResponse;
import com.commerceinsight.customer.service.CustomerGroupService;
import com.commerceinsight.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CustomerGroupController — REST API for customer group management.
 *
 * <p>Base path: /api/v1/customer-groups
 */
@Tag(name = "Customer Groups", description = "Customer group management endpoints")
@RestController
@RequestMapping("/api/v1/customer-groups")
@RequiredArgsConstructor
public class CustomerGroupController {

    private final CustomerGroupService groupService;

    @Operation(summary = "List all customer groups (paginated)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Page<CustomerGroupResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Page<CustomerGroupResponse> result = groupService.findAll(PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(result, "Customer groups retrieved successfully"));
    }

    @Operation(summary = "Get a customer group by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerGroupResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(groupService.findById(id), "Customer group retrieved successfully"));
    }

    @Operation(summary = "Create a new customer group")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerGroupResponse>> create(
            @Valid @RequestBody CreateCustomerGroupRequest request) {
        CustomerGroupResponse response = groupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Customer group created successfully"));
    }

    @Operation(summary = "Update an existing customer group")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerGroupResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerGroupRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(groupService.update(id, request), "Customer group updated successfully"));
    }

    @Operation(summary = "Delete a customer group")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
