package com.commerceinsight.user.dto.response;

import com.commerceinsight.user.domain.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * UserResponse — public representation of a platform user.
 *
 * <p>Used by:
 * <ul>
 *   <li>GET /api/v1/users — admin user list</li>
 *   <li>GET /api/v1/users/{id} — admin user detail</li>
 *   <li>GET /api/v1/auth/me — current user profile</li>
 * </ul>
 *
 * <p>Architecture Rule: This DTO MUST NEVER expose passwordHash.
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
    private final int failedAttempts;
    private final Instant lastLoginAt;
    private final Instant createdAt;
}
