package com.commerceinsight.auth.dto.response;

import com.commerceinsight.user.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * UserResponse — public representation of a platform user.
 *
 * <p>Returned as part of {@link AuthResponse} and from GET /api/v1/auth/me.
 *
 * <p>Architecture Rule: This DTO must NEVER expose the passwordHash field.
 * It is a strict subset of the User entity, safe for client consumption.
 */
@Getter
@Builder
public class UserResponse {

    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String fullName;
    private final Role role;
    private final boolean active;
    private final boolean locked;
    private final Instant lastLoginAt;
    private final Instant createdAt;
}
