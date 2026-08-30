package com.commerceinsight.auth;

import com.commerceinsight.auth.domain.RefreshToken;
import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.auth.mapper.AuthMapper;
import com.commerceinsight.auth.service.AuthService;
import com.commerceinsight.auth.service.LoginHistoryService;
import com.commerceinsight.auth.service.RefreshTokenService;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.security.JwtTokenUtil;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.dto.response.UserResponse;
import com.commerceinsight.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthServiceTest — unit tests for {@link AuthService}.
 *
 * <p>All dependencies are mocked with Mockito.
 * No Spring context is loaded — this tests pure business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private AuthMapper authMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditLogService auditLogService;
    @Mock private LoginHistoryService loginHistoryService;

    @InjectMocks
    private AuthService authService;

    // Test fixtures
    private User testUser;
    private UserResponse testUserResponse;
    private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test";
    private static final String TEST_REFRESH_TOKEN = "test-refresh-token-uuid";
    private static final long EXPIRES_IN = 900L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$12$hashed")
                .firstName("John")
                .lastName("Doe")
                .role(Role.STAFF)
                .active(true)
                .locked(false)
                .failedAttempts(0)
                .build();
        // Manually set ID since it's normally set by JPA
        setFieldValue(testUser, "id", UUID.randomUUID());

        testUserResponse = UserResponse.builder()
                .id(testUser.getId())
                .email(testUser.getEmail())
                .firstName(testUser.getFirstName())
                .lastName(testUser.getLastName())
                .role(testUser.getRole())
                .active(true)
                .build();
    }

    // ── Register Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully and return AuthResponse with user")
        void register_success() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "John", "Doe", "new@example.com", "SecurePass@123");

            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("$2a$12$hashed");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            given(jwtTokenUtil.generateAccessToken(any(), any(), any(), any()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtTokenUtil.getAccessTokenExpirationSeconds()).willReturn(EXPIRES_IN);
            given(refreshTokenService.createRefreshToken(any())).willReturn(TEST_REFRESH_TOKEN);
            given(authMapper.toUserResponse(any(User.class))).willReturn(testUserResponse);

            // When
            AuthResponse result = authService.register(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.getExpiresIn()).isEqualTo(EXPIRES_IN);
            assertThat(result.getUser()).isNotNull();
            assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");

            then(userRepository).should().save(any(User.class));
            then(refreshTokenService).should().createRefreshToken(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void register_duplicateEmail_throwsException() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "Jane", "Smith", "existing@example.com", "SecurePass@123");
            given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

            // When / Then
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("existing@example.com");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should assign STAFF role to newly registered users")
        void register_assignsStaffRole() {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "Alice", "Walker", "alice@example.com", "SecurePass@123");
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("hashed");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getRole()).isEqualTo(Role.STAFF);
                setFieldValue(u, "id", UUID.randomUUID());
                return u;
            });
            given(jwtTokenUtil.generateAccessToken(any(), any(), any(), any()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtTokenUtil.getAccessTokenExpirationSeconds()).willReturn(EXPIRES_IN);
            given(refreshTokenService.createRefreshToken(any())).willReturn(TEST_REFRESH_TOKEN);
            given(authMapper.toUserResponse(any(User.class))).willReturn(testUserResponse);

            // When
            authService.register(request);

            // Then — verified inside the save() answer above
        }
    }

    // ── Login Tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return AuthResponse with user")
        void login_success() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(authenticationManager.authenticate(any())).willReturn(
                    new UsernamePasswordAuthenticationToken(testUser.getId().toString(), null));
            given(jwtTokenUtil.generateAccessToken(any(), any(), any(), any()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtTokenUtil.getAccessTokenExpirationSeconds()).willReturn(EXPIRES_IN);
            given(refreshTokenService.createRefreshToken(any())).willReturn(TEST_REFRESH_TOKEN);
            given(authMapper.toUserResponse(any(User.class))).willReturn(testUserResponse);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            AuthResponse result = authService.login(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.getUser()).isNotNull();
            // failedAttempts reset
            assertThat(testUser.getFailedAttempts()).isEqualTo(0);
            // lastLoginAt was set
            assertThat(testUser.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw BadCredentialsException for non-existent user")
        void login_userNotFound_throwsBadCredentials() {
            // Given
            LoginRequest request = new LoginRequest("nobody@example.com", "password");
            given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("should throw LockedException when account is locked")
        void login_lockedAccount_throwsLocked() {
            // Given
            testUser.setLocked(true);
            LoginRequest request = new LoginRequest("test@example.com", "password");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

            // When / Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(LockedException.class);
        }

        @Test
        @DisplayName("should throw DisabledException when account is inactive")
        void login_inactiveAccount_throwsDisabled() {
            // Given
            testUser.setActive(false);
            LoginRequest request = new LoginRequest("test@example.com", "password");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

            // When / Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(DisabledException.class);
        }

        @Test
        @DisplayName("should increment failed attempts on bad credentials")
        void login_badCredentials_incrementsFailedAttempts() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When / Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            assertThat(testUser.getFailedAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("should lock account after 5 consecutive failed attempts")
        void login_fiveFailedAttempts_locksAccount() {
            // Given
            testUser.setFailedAttempts(4); // One more attempt will lock it
            LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When / Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            assertThat(testUser.isLocked()).isTrue();
            assertThat(testUser.getFailedAttempts()).isEqualTo(5);
        }
    }

    // ── Refresh Tests ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class RefreshTests {

        @Test
        @DisplayName("should issue new tokens on valid refresh token")
        void refresh_success() {
            // Given
            RefreshToken oldToken = RefreshToken.builder()
                    .user(testUser)
                    .familyId(UUID.randomUUID())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            given(refreshTokenService.validateAndRotate(eq("valid-token"), any())).willReturn(oldToken);
            given(refreshTokenService.createRefreshTokenInFamily(any(), any()))
                    .willReturn(TEST_REFRESH_TOKEN);
            given(jwtTokenUtil.generateAccessToken(any(), any(), any(), any()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtTokenUtil.getAccessTokenExpirationSeconds()).willReturn(EXPIRES_IN);

            // When
            AuthResponse result = authService.refresh("valid-token");

            // Then
            assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.getUser()).isNull(); // Token-only response
        }

        @Test
        @DisplayName("should propagate BusinessRuleException on invalid refresh token")
        void refresh_invalidToken_throwsException() {
            // Given
            given(refreshTokenService.validateAndRotate(eq("invalid"), any()))
                    .willThrow(new BusinessRuleException(
                            com.commerceinsight.shared.exception.ErrorCode.REFRESH_TOKEN_INVALID,
                            "Invalid refresh token"));

            // When / Then
            assertThatThrownBy(() -> authService.refresh("invalid"))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ── Logout Tests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class LogoutTests {

        @Test
        @DisplayName("should revoke all refresh tokens for the user")
        void logout_success() {
            // Given
            UUID userId = UUID.randomUUID();
            given(refreshTokenService.revokeAllForUser(userId)).willReturn(3);

            // When
            authService.logout(userId);

            // Then
            then(refreshTokenService).should().revokeAllForUser(userId);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /**
     * Reflection helper to set private/inherited fields for testing.
     */
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            // Try the class itself first, then superclasses
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    var field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access field: " + fieldName, e);
        }
    }
}
