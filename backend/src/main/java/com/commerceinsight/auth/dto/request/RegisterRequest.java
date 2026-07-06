package com.commerceinsight.auth.dto.request;

import jakarta.validation.constraints.*;

/**
 * RegisterRequest — request body for POST /api/v1/auth/register.
 *
 * <p>Password policy as per docs/06_AUTHENTICATION.md §10:
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one uppercase, one lowercase, one digit, one special character</li>
 * </ul>
 */
public record RegisterRequest(

        @NotBlank(message = "First name must not be blank")
        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
        String firstName,

        @NotBlank(message = "Last name must not be blank")
        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
        String lastName,

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&^#])[A-Za-z\\d@$!%*?&^#]{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String password

) {}
