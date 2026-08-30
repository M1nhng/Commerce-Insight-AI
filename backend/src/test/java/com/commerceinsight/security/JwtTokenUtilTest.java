package com.commerceinsight.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * JwtTokenUtilTest — signature, expiry, issuer, token-type and weak-key checks.
 */
@DisplayName("JwtTokenUtil Unit Tests")
class JwtTokenUtilTest {

    // Deliberately NOT valid Base64 ('-' / '!') so JwtTokenUtil.decodeSecret()
    // and the test's key() both use the raw UTF-8 bytes → identical signing key.
    private static final String SECRET =
            "commerce-insight-unit-test-signing-secret-well-over-32-bytes!!";
    private static final String ISSUER = "commerce-insight-ai";

    private JwtTokenUtil util;
    private final UUID userId = UUID.randomUUID();

    private UserDetails userDetails() {
        return User.builder().username(userId.toString()).password("x").authorities("ROLE_STAFF").build();
    }

    private JwtTokenUtil newUtil(String secret, long ttlMs, String issuer) {
        JwtTokenUtil u = new JwtTokenUtil();
        ReflectionTestUtils.setField(u, "jwtSecret", secret);
        ReflectionTestUtils.setField(u, "accessTokenExpirationMs", ttlMs);
        ReflectionTestUtils.setField(u, "issuer", issuer);
        return u;
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        util = newUtil(SECRET, 900_000L, ISSUER);
    }

    @Test
    @DisplayName("round-trips a valid access token (subject + typ)")
    void validToken_roundTrips() {
        String token = util.generateAccessToken(userDetails(), userId, "u@example.com",
                List.of("ROLE_STAFF"));
        assertThat(util.extractSubject(token)).isEqualTo(userId.toString());
        assertThat(util.isTokenValid(token, userDetails())).isTrue();
    }

    @Test
    @DisplayName("rejects a token with no 'typ' claim")
    void missingTypeClaim_rejected() {
        String noType = Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(SECRET))
                .compact();
        assertThat(util.isTokenValid(noType, userDetails())).isFalse();
        assertThatThrownBy(() -> util.extractSubject(noType))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("rejects an expired token")
    void expiredToken_rejected() {
        JwtTokenUtil shortLived = newUtil(SECRET, -1_000L, ISSUER); // already expired
        String token = shortLived.generateAccessToken(userDetails(), userId, "u@example.com",
                List.of("ROLE_STAFF"));
        assertThat(shortLived.isTokenValid(token, userDetails())).isFalse();
    }

    @Test
    @DisplayName("rejects a token signed with a different key")
    void badSignature_rejected() {
        String foreign = Jwts.builder()
                .subject(userId.toString())
                .issuer(ISSUER)
                .claim("typ", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key("a-totally-different-secret-key-of-at-least-32-bytes!!"))
                .compact();
        assertThat(util.isTokenValid(foreign, userDetails())).isFalse();
    }

    @Test
    @DisplayName("rejects a token with the wrong issuer")
    void wrongIssuer_rejected() {
        JwtTokenUtil other = newUtil(SECRET, 900_000L, "some-other-issuer");
        String token = other.generateAccessToken(userDetails(), userId, "u@example.com",
                List.of("ROLE_STAFF"));
        assertThat(util.isTokenValid(token, userDetails())).isFalse();
    }

    @Test
    @DisplayName("@PostConstruct rejects a sub-256-bit secret")
    void weakSecret_failsFast() {
        JwtTokenUtil weak = newUtil("too-short", 900_000L, ISSUER);
        Throwable t = catchThrowable(weak::validateSigningKey);
        assertThat(t).isInstanceOf(IllegalStateException.class);
        assertThat(t.getMessage()).contains("256 bits");
    }

    @Test
    @DisplayName("@PostConstruct accepts a strong secret")
    void strongSecret_ok() {
        assertThat(catchThrowable(util::validateSigningKey)).isNull();
    }
}
