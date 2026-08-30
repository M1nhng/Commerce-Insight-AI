package com.commerceinsight.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/**
 * JwtTokenUtil — JWT creation, parsing, and validation utility.
 *
 * <p>Architecture: This is an infrastructure utility, not a business service.
 * It is only called by {@link JwtAuthenticationFilter} and {@link com.commerceinsight.auth.service.AuthService}.
 *
 * <p>Token payload (claims):
 * <ul>
 *   <li>sub — User UUID (primary key)</li>
 *   <li>iss — Issuer: "commerce-insight-ai"</li>
 *   <li>iat — Issued-at timestamp</li>
 *   <li>exp — Expiration timestamp</li>
 *   <li>roles — List of granted roles (e.g., ["ROLE_ADMIN"])</li>
 *   <li>email — User email (for display purposes only)</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenUtil {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";

    /** HS256 requires a key of at least 256 bits. */
    private static final int MIN_KEY_BYTES = 32;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

    /**
     * Validate the signing key at startup so a weak/short secret fails fast with
     * a clear message rather than lazily on the first token operation.
     */
    @PostConstruct
    void validateSigningKey() {
        byte[] keyBytes = decodeSecret();
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret is too short: HS256 requires a key of at least "
                            + MIN_KEY_BYTES + " bytes (256 bits), got " + keyBytes.length + ".");
        }
        log.info("JwtTokenUtil: signing key OK ({} bytes), issuer='{}', accessTtl={}s",
                keyBytes.length, issuer, accessTokenExpirationMs / 1000);
    }

    // ── Token Generation ────────────────────────────────────────────────

    /**
     * Generate a signed JWT access token for the given user.
     *
     * @param userDetails the authenticated user details
     * @param userId      the user's UUID (used as JWT subject)
     * @param email       the user's email (stored as claim)
     * @param roles       list of role strings (e.g., ["ROLE_ADMIN"])
     * @return signed JWT string
     */
    public String generateAccessToken(UserDetails userDetails, UUID userId,
                                      String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .id(UUID.randomUUID().toString())          // jti — for log correlation
                .issuedAt(now)
                .expiration(expiry)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_EMAIL, email)
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token Parsing ────────────────────────────────────────────────────

    /**
     * Extract the subject (user UUID) from a JWT token.
     *
     * @param token the raw JWT string
     * @return the subject (user UUID as string)
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the expiration date from a JWT token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract roles list from the JWT claims.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get(CLAIM_ROLES));
    }

    /**
     * Extract email from the JWT claims.
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> (String) claims.get(CLAIM_EMAIL));
    }

    /**
     * Generic claim extractor using a resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ── Token Validation ─────────────────────────────────────────────────

    /**
     * Validate the token: checks signature, expiry, and that subject matches userDetails.
     *
     * @param token       the JWT string
     * @param userDetails the UserDetails to validate against
     * @return true if valid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String subject = extractSubject(token);
            return subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Check if a token is expired without throwing an exception.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * Return the access token expiration in seconds (for the expiresIn field in AuthResponse).
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .require(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)   // reject refresh/other token types
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(decodeSecret());
    }

    /**
     * Decode the configured secret: Base64 first, falling back to raw UTF-8
     * bytes for plain-text secrets (dev/test). Length is enforced in
     * {@link #validateSigningKey()}.
     */
    private byte[] decodeSecret() {
        try {
            return Decoders.BASE64.decode(jwtSecret);
        } catch (RuntimeException ex) {
            // Not valid Base64 (e.g. a plain-text dev secret) — use the raw bytes.
            return jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
