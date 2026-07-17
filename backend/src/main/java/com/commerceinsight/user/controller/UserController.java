package com.commerceinsight.user.controller;

import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.user.dto.request.ChangeRoleRequest;
import com.commerceinsight.user.dto.request.CreateUserRequest;
import com.commerceinsight.user.dto.request.UpdateUserRequest;
import com.commerceinsight.user.dto.response.UserResponse;
import com.commerceinsight.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * UserController — HTTP adapter for user management endpoints.
 *
 * <p>All endpoints are restricted to ADMIN role only.
 *
 * <p>Architecture Rules (STRICTLY ENFORCED):
 * <ul>
 *   <li>This controller contains ZERO business logic.</li>
 *   <li>It only: validates input (@Valid), delegates to UserService, wraps response.</li>
 *   <li>It NEVER accesses any repository or domain entity directly.</li>
 *   <li>All responses use {@link ApiResponse} envelope.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "User Management", description = "ADMIN-only user CRUD and role management")
public class UserController {

    private final UserService userService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * GET /api/v1/users
     *
     * <p>Paginated list of all platform users.
     */
    @GetMapping
    @Operation(summary = "List all users (ADMIN)",
               description = "Returns a paginated list of all active users. ADMIN only.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        PageResponse<UserResponse> page = PageResponse.from(userService.listUsers(pageable));
        return ResponseEntity.ok(ApiResponse.success(page, "Users retrieved successfully"));
    }

    /**
     * GET /api/v1/users/{id}
     *
     * <p>Fetch a single user by UUID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (ADMIN)",
               description = "Returns a single user's full profile. ADMIN only.")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    /**
     * POST /api/v1/users
     *
     * <p>Admin creates a new user with any role.
     */
    @PostMapping
    @Operation(summary = "Create a user (ADMIN)",
               description = "Creates a new user account with the specified role. ADMIN only.")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UUID adminId = securityContextHelper.getCurrentUserId();
        UserResponse created = userService.createUser(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "User created successfully"));
    }

    /**
     * PUT /api/v1/users/{id}
     *
     * <p>Admin updates a user's profile fields.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a user (ADMIN)",
               description = "Updates firstName, lastName, email, and active status. ADMIN only.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {

        UUID adminId = securityContextHelper.getCurrentUserId();
        UserResponse updated = userService.updateUser(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(updated, "User updated successfully"));
    }

    /**
     * PATCH /api/v1/users/{id}/role
     *
     * <p>Admin changes a user's role. An admin cannot change their own role.
     */
    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role (ADMIN)",
               description = "Changes a user's role. An admin cannot change their own role. ADMIN only.")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request) {

        UUID adminId = securityContextHelper.getCurrentUserId();
        UserResponse updated = userService.changeRole(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(updated, "User role updated successfully"));
    }

    /**
     * PATCH /api/v1/users/{id}/unlock
     *
     * <p>Admin manually unlocks a locked user account.
     */
    @PatchMapping("/{id}/unlock")
    @Operation(summary = "Unlock a user account (ADMIN)",
               description = "Manually unlocks a user account that was locked due to failed login attempts.")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable UUID id) {
        UUID adminId = securityContextHelper.getCurrentUserId();
        UserResponse updated = userService.unlockUser(id, adminId);
        return ResponseEntity.ok(ApiResponse.success(updated, "User account unlocked successfully"));
    }

    /**
     * DELETE /api/v1/users/{id}
     *
     * <p>Admin soft-deletes a user. The record is retained in the database.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user (ADMIN)",
               description = "Soft-deletes a user account. The record is retained for audit purposes. ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        UUID adminId = securityContextHelper.getCurrentUserId();
        userService.deleteUser(id, adminId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
