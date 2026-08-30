package com.commerceinsight.auth.service;

import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.auth.domain.RefreshToken;
import com.commerceinsight.auth.repository.RefreshTokenRepository;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * RefreshTokenService — manages the lifecycle of JWT refresh tokens.
 *
 * <p>Implements Token Rotation: each use of a refresh token generates
 * a new one and revokes the old. Token reuse (presenting an already-revoked token)
 * triggers family-wide revocation as a security measure.
 *
 * <p>Architecture Rule: Only {@link AuthService} should call this service.
 * No controller or other module interacts with refresh tokens directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private int refreshTokenExpirationDays;

    // ── Token Creation ────────────────────────────────────────────────────

    /**
     * Create and persist a new refresh token for the given user.
     *
     * <p>A new family ID is created for each login. Rotated tokens share the same family.
     *
     * @param user the user to create the token for
     * @return the plain (unhashed) refresh token UUID — return this to the client
     */
    @Transactional
    public String createRefreshToken(User user) {
        return createRefreshTokenInFamily(user, UUID.randomUUID());
    }

    /**
     * Create a new refresh token within an existing family (rotation).
     *
     * @param user     the user
     * @param familyId the existing rotation family ID
     * @return the plain refresh token UUID string
     */
    @Transactional
    public String createRefreshTokenInFamily(User user, UUID familyId) {
        String plainToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(plainToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .familyId(familyId)
                .expiresAt(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token for user {} in family {}", user.getId(), familyId);
        return plainToken;
    }

    // ── Token Validation & Rotation ───────────────────────────────────────

    /**
     * Validate a presented refresh token.
     *
     * <p>Token rotation logic:
     * <ol>
     *   <li>Hash the presented token.</li>
     *   <li>Look up the token record by hash.</li>
     *   <li>If not found → reject (invalid).</li>
     *   <li>If already revoked → REUSE DETECTED → revoke entire family → reject.</li>
     *   <li>If expired → reject.</li>
     *   <li>If valid → revoke old token, return the token entity.</li>
     * </ol>
     *
     * @param plainToken the raw refresh token presented by the client
     * @return the valid {@link RefreshToken} entity (not yet rotated — caller creates new)
     * @throws BusinessRuleException if token is invalid, expired, or reuse detected
     */
    @Transactional
    public RefreshToken validateAndRotate(String plainToken) {
        return validateAndRotate(plainToken, null);
    }

    @Transactional
    public RefreshToken validateAndRotate(String plainToken, String ipAddress) {
        String tokenHash = hashToken(plainToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found (invalid or tampered)");
                    return new BusinessRuleException(ErrorCode.REFRESH_TOKEN_INVALID,
                            "Invalid refresh token");
                });

        // Reuse detection: token already revoked
        if (token.isRevoked()) {
            UUID userId = token.getUser().getId();
            log.warn("SECURITY: Refresh token reuse detected for user {}! " +
                    "Revoking entire family {}.", userId, token.getFamilyId());
            refreshTokenRepository.revokeAllByFamilyId(token.getFamilyId(), Instant.now());
            auditLogService.log(userId, AuditLogService.ACTION_REFRESH_TOKEN_REUSE, ipAddress);
            throw new BusinessRuleException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED,
                    "Refresh token has already been used. Please log in again.");
        }

        // Expiry check
        if (!token.isValid()) {
            log.debug("Refresh token expired for user {}", token.getUser().getId());
            throw new BusinessRuleException(ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token has expired. Please log in again.");
        }

        // Revoke the current token (it's being rotated)
        token.revoke();
        refreshTokenRepository.save(token);

        return token;
    }

    // ── Token Revocation ─────────────────────────────────────────────────

    /**
     * Revoke all refresh tokens for a user (called on logout).
     *
     * @param userId the user's UUID
     * @return number of tokens revoked
     */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.debug("Revoked {} refresh tokens for user {}", count, userId);
        return count;
    }

    // ── Internal Utilities ────────────────────────────────────────────────

    /**
     * Hash a plain refresh token using SHA-256.
     * Only the hash is stored in the database.
     *
     * @param plainToken the plain UUID token string
     * @return lowercase hex-encoded SHA-256 hash
     */
    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
