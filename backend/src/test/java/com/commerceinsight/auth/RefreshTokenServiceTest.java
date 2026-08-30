package com.commerceinsight.auth;

import com.commerceinsight.auth.domain.RefreshToken;
import com.commerceinsight.auth.repository.RefreshTokenRepository;
import com.commerceinsight.auth.service.RefreshTokenService;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.domain.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * RefreshTokenServiceTest — unit tests for {@link RefreshTokenService}.
 *
 * <p>Tests all token lifecycle scenarios:
 * token creation, validation, rotation, reuse detection, and bulk revocation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private com.commerceinsight.admin.service.AuditLogService auditLogService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationDays", 7);

        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .role(Role.STAFF)
                .build();
        ReflectionTestUtils.setField(testUser, "id", UUID.randomUUID());
    }

    // ── createRefreshToken ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createRefreshToken()")
    class CreateTokenTests {

        @Test
        @DisplayName("should create a token and return the plain UUID string")
        void createToken_returnsPlainToken() {
            // Given
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // When
            String token = refreshTokenService.createRefreshToken(testUser);

            // Then
            assertThat(token).isNotBlank();
            // Should be parseable as UUID
            assertThatCode(() -> UUID.fromString(token)).doesNotThrowAnyException();

            // Verify the persisted token has a hash (not the plain token)
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            then(refreshTokenRepository).should().save(captor.capture());
            RefreshToken saved = captor.getValue();

            assertThat(saved.getTokenHash()).isNotEqualTo(token); // Hash, not plain token
            assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex = 64 chars
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getFamilyId()).isNotNull();
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("should create tokens in the same family when familyId is provided")
        void createTokenInFamily_sameFamily() {
            // Given
            UUID familyId = UUID.randomUUID();
            given(refreshTokenRepository.save(any(RefreshToken.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // When
            refreshTokenService.createRefreshTokenInFamily(testUser, familyId);

            // Then
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            then(refreshTokenRepository).should().save(captor.capture());
            assertThat(captor.getValue().getFamilyId()).isEqualTo(familyId);
        }
    }

    // ── validateAndRotate ─────────────────────────────────────────────────

    @Nested
    @DisplayName("validateAndRotate()")
    class ValidateAndRotateTests {

        @Test
        @DisplayName("should return the valid token and mark it as revoked")
        void validate_validToken_revokesAndReturns() {
            // Given — create a plain token, then look up its hash
            String plainToken = UUID.randomUUID().toString();
            String expectedHash = sha256(plainToken);

            RefreshToken validToken = RefreshToken.builder()
                    .tokenHash(expectedHash)
                    .user(testUser)
                    .familyId(UUID.randomUUID())
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByTokenHash(expectedHash))
                    .willReturn(Optional.of(validToken));
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // When
            RefreshToken result = refreshTokenService.validateAndRotate(plainToken);

            // Then
            assertThat(result).isEqualTo(validToken);
            assertThat(validToken.isRevoked()).isTrue();
            assertThat(validToken.getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw REFRESH_TOKEN_INVALID for unknown token")
        void validate_unknownToken_throwsInvalid() {
            // Given
            String plainToken = UUID.randomUUID().toString();
            given(refreshTokenRepository.findByTokenHash(anyString()))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateAndRotate(plainToken))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        @Test
        @DisplayName("should throw REFRESH_TOKEN_REUSE_DETECTED and revoke family on reuse")
        void validate_revokedToken_detectsReuseAndRevokesFamily() {
            // Given
            String plainToken = UUID.randomUUID().toString();
            String tokenHash = sha256(plainToken);
            UUID familyId = UUID.randomUUID();

            RefreshToken revokedToken = RefreshToken.builder()
                    .tokenHash(tokenHash)
                    .user(testUser)
                    .familyId(familyId)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .revoked(true) // ALREADY REVOKED
                    .build();

            given(refreshTokenRepository.findByTokenHash(tokenHash))
                    .willReturn(Optional.of(revokedToken));
            given(refreshTokenRepository.revokeAllByFamilyId(eq(familyId), any()))
                    .willReturn(2);

            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateAndRotate(plainToken))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

            // Entire family must be revoked
            then(refreshTokenRepository).should().revokeAllByFamilyId(eq(familyId), any());
        }

        @Test
        @DisplayName("should throw REFRESH_TOKEN_EXPIRED for expired token")
        void validate_expiredToken_throwsExpired() {
            // Given
            String plainToken = UUID.randomUUID().toString();
            String tokenHash = sha256(plainToken);

            RefreshToken expiredToken = RefreshToken.builder()
                    .tokenHash(tokenHash)
                    .user(testUser)
                    .familyId(UUID.randomUUID())
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS)) // EXPIRED
                    .revoked(false)
                    .build();

            given(refreshTokenRepository.findByTokenHash(tokenHash))
                    .willReturn(Optional.of(expiredToken));

            // When / Then
            assertThatThrownBy(() -> refreshTokenService.validateAndRotate(plainToken))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting(ex -> ((BusinessRuleException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    // ── revokeAllForUser ──────────────────────────────────────────────────

    @Nested
    @DisplayName("revokeAllForUser()")
    class RevokeAllTests {

        @Test
        @DisplayName("should revoke all tokens for the given user ID")
        void revokeAll_callsRepository() {
            // Given
            UUID userId = UUID.randomUUID();
            given(refreshTokenRepository.revokeAllByUserId(eq(userId), any())).willReturn(3);

            // When
            int count = refreshTokenService.revokeAllForUser(userId);

            // Then
            assertThat(count).isEqualTo(3);
            then(refreshTokenRepository).should().revokeAllByUserId(eq(userId), any(Instant.class));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** SHA-256 hex helper to match the service's internal hashing. */
    private String sha256(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
