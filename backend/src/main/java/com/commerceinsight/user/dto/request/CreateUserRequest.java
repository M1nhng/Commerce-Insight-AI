package com.commerceinsight.user.dto.request;

import jakarta.validation.constraints.*;

/**
 * CreateUserRequest — request body for ADMIN creating a new user.
 *
 * <p>Unlike self-registration, an admin can set any role.
 * Endpoint: POST /api/v1/users
 */
public record CreateUserRequest(

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

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
        )
        String password,

        @NotNull(message = "Role is required")
        com.commerceinsight.user.domain.Role role
) {}
