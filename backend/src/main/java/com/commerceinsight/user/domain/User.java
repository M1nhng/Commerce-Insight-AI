package com.commerceinsight.user.domain;

import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * User — the platform user account entity.
 *
 * <p>Maps to the {@code users} table in PostgreSQL.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never return this entity directly from a controller. Use UserResponse DTO.</li>
 *   <li>Never set passwordHash from a plain password — always hash first via BCrypt.</li>
 *   <li>Soft delete is enforced via @SQLRestriction — deleted users are invisible to all queries.</li>
 * </ul>
 *
 * <p>Soft delete: {@code deleted_at IS NULL} — set deletedAt to remove logically.
 */
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /**
     * Unique email address used for authentication.
     * Uniqueness enforced at DB level (partial index on deleted_at IS NULL).
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * BCrypt-hashed password. Never store or expose plaintext.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * User's assigned role. Stored as VARCHAR(50) in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    /**
     * Whether the account is active. Inactive accounts cannot log in.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Whether the account is locked due to too many failed login attempts.
     */
    @Column(name = "locked", nullable = false)
    @Builder.Default
    private boolean locked = false;

    /**
     * Counter for consecutive failed login attempts.
     * Reset to 0 on successful login.
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    /**
     * Timestamp of the most recent successful login.
     */
    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;

    // ── Convenience Methods ──────────────────────────────────────────────

    /**
     * Returns the user's full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Increment the failed login attempt counter.
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    /**
     * Reset failed attempts after a successful login.
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }
}
