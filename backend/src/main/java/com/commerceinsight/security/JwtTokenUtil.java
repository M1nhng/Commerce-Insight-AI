package com.commerceinsight.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.issuer}")
    private String issuer;

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
                .issuedAt(now)
                .expiration(expiry)
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
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            // If not Base64, use the raw bytes (for plain-text secrets in dev)
            keyBytes = jwtSecret.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
