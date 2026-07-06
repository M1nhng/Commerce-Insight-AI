package com.commerceinsight.auth.repository;

import com.commerceinsight.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshTokenRepository — data access for {@link RefreshToken} entities.
 *
 * <p>Architecture Rule: Only {@link com.commerceinsight.auth.service.RefreshTokenService}
 * may inject this repository.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a refresh token by its SHA-256 hash.
     * Used to look up a presented refresh token.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all refresh tokens belonging to a token family.
     * Used for family revocation on token reuse detection.
     */
    List<RefreshToken> findByFamilyId(UUID familyId);

    /**
     * Find all active (non-revoked, non-expired) tokens for a user.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") UUID userId,
                                                @Param("now") Instant now);

    /**
     * Revoke all refresh tokens for a user (used on logout).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Revoke all tokens in a family (used on reuse detection).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.familyId = :familyId AND rt.revoked = false")
    int revokeAllByFamilyId(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /**
     * Delete all expired and revoked tokens (for cleanup/maintenance).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff AND rt.revoked = true")
    int deleteExpiredAndRevoked(@Param("cutoff") Instant cutoff);
}
