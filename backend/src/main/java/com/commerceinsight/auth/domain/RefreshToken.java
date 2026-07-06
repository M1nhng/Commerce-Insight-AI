package com.commerceinsight.auth.domain;

import com.commerceinsight.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * RefreshToken — persisted JWT refresh token record.
 *
 * <p>Maps to the {@code refresh_tokens} table.
 *
 * <p>Architecture: Only the SHA-256 hash of the actual token is stored.
 * The plain token is returned to the client and never persisted.
 *
 * <p>Token rotation: Each use of a refresh token creates a new one and revokes the old.
 * Token reuse (using an already-revoked token) triggers family revocation
 * to protect against token theft.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * SHA-256 hex hash of the actual refresh token UUID.
     * Stored instead of the plain token for security.
     */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    /**
     * The user this token belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Token family ID — all tokens derived from the same login share a family.
     * Used for reuse detection: if a revoked token's family is found, revoke all.
     */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    /**
     * When this refresh token expires.
     * Tokens past this time are considered expired even if not revoked.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Whether this token has been revoked (used or explicitly invalidated).
     * Revoked tokens must be rejected immediately.
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Timestamp when this token was revoked. Null if still active.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    // ── Convenience Methods ──────────────────────────────────────────────

    /**
     * Check if the token is valid (not revoked and not expired).
     */
    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    /**
     * Revoke this token by setting revoked = true and recording the timestamp.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
