package com.commerceinsight.user.dto.request;

import com.commerceinsight.user.domain.Role;
import jakarta.validation.constraints.NotNull;

/**
 * ChangeRoleRequest — request body for ADMIN changing a user's role.
 *
 * <p>Security Rule: An admin cannot change their own role.
 * This is enforced in UserService, not here.
 * Endpoint: PATCH /api/v1/users/{id}/role
 */
public record ChangeRoleRequest(

        @NotNull(message = "Role is required")
        Role role
) {}
