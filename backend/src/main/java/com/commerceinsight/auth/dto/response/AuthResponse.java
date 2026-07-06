package com.commerceinsight.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * AuthResponse — response body for login, register, and refresh endpoints.
 *
 * <p>Wire format:
 * <pre>
 * {
 *   "accessToken": "eyJ...",
 *   "refreshToken": "550e8400-...",
 *   "expiresIn": 900,
 *   "user": { ... }
 * }
 * </pre>
 *
 * <p>The {@code user} field is null in refresh responses (only tokens are returned).
 * {@code @JsonInclude(NON_NULL)} ensures null user is omitted from the wire.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /** Signed JWT access token. Expires in {@code expiresIn} seconds. */
    private final String accessToken;

    /**
     * Opaque UUID refresh token. Store securely.
     * Exchange for a new access token at POST /api/v1/auth/refresh.
     */
    private final String refreshToken;

    /** Access token expiry in seconds (typically 900 = 15 minutes). */
    private final long expiresIn;

    /**
     * The authenticated user's profile.
     * Present in login/register responses; omitted in refresh responses.
     */
    private final UserResponse user;

    // ── Factory convenience methods ───────────────────────────────────────

    /**
     * Full auth response (login/register) — includes user profile.
     */
    public static AuthResponse full(String accessToken, String refreshToken,
                                    long expiresIn, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }

    /**
     * Token-only response (refresh) — no user profile included.
     */
    public static AuthResponse tokenOnly(String accessToken, String refreshToken, long expiresIn) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();
    }
}
