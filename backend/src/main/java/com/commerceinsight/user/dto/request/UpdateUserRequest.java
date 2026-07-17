package com.commerceinsight.user.dto.request;

import jakarta.validation.constraints.*;

/**
 * UpdateUserRequest — request body for ADMIN updating a user's profile.
 *
 * <p>Role changes are handled separately via {@link ChangeRoleRequest}.
 * Endpoint: PUT /api/v1/users/{id}
 */
public record UpdateUserRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotNull(message = "Active status is required")
        Boolean active
) {}
