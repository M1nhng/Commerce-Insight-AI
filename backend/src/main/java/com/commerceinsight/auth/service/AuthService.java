package com.commerceinsight.auth.service;

import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.auth.domain.RefreshToken;
import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.auth.mapper.AuthMapper;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.security.JwtTokenUtil;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.dto.response.UserResponse;
import com.commerceinsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AuthService — authentication business logic.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Register new users</li>
 *   <li>Authenticate existing users (login)</li>
 *   <li>Refresh access tokens via refresh token rotation</li>
 *   <li>Logout (revoke all refresh tokens)</li>
 *   <li>Fetch the currently authenticated user's profile</li>
 * </ul>
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>All business logic lives here — controllers are thin HTTP adapters.</li>
 *   <li>Never return User entity — always map to UserResponse via AuthMapper.</li>
 *   <li>Never expose passwordHash in any DTO.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;

    // ── Register ─────────────────────────────────────────────────────────

    /**
     * Register a new user account.
     *
     * <p>New users are assigned the STAFF role by default.
     * Admins can later change roles via the User Management API.
     *
     * @param request the registration request
     * @return a full AuthResponse with access token, refresh token, and user profile
     * @throws DuplicateResourceException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.email())) {
            throw DuplicateResourceException.email(request.email());
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .role(Role.STAFF)  // Default role for self-registration
                .active(true)
                .locked(false)
                .failedAttempts(0)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} ({})", savedUser.getEmail(), savedUser.getId());

        auditLogService.log(savedUser.getId(), AuditLogService.ACTION_USER_CREATED,
                "User", savedUser.getId(), null,
                String.format("{\"email\":\"%s\"}", savedUser.getEmail()), ipAddress);

        return buildAuthResponse(savedUser, true);
    }

    // ── Login ────────────────────────────────────────────────────────────

    /**
     * Authenticate a user by email and password.
     *
     * <p>Login logic:
     * <ol>
     *   <li>Verify account is not locked or disabled.</li>
     *   <li>Delegate credential verification to Spring Security AuthenticationManager.</li>
     *   <li>On success: reset failed attempts, update lastLoginAt, issue tokens.</li>
     *   <li>On failure: increment failed attempts, lock after MAX_FAILED_ATTEMPTS.</li>
     * </ol>
     *
     * @param request the login request
     * @return a full AuthResponse with access token, refresh token, and user profile
     * @throws BadCredentialsException if credentials are wrong
     * @throws LockedException         if account is locked
     * @throws DisabledException       if account is inactive
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Pre-auth checks
        if (user.isLocked()) {
            throw new LockedException("Account is locked. Please contact an administrator.");
        }
        if (!user.isActive()) {
            throw new DisabledException("Account is deactivated.");
        }

        try {
            // Delegate to Spring Security (BCrypt verification + UserDetailsService)
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getId().toString(),  // username = UUID (per UserDetailsServiceImpl)
                            request.password()
                    )
            );

            // Success — reset failed attempts, record login time
            user.resetFailedAttempts();
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            log.info("User logged in: {} ({})", user.getEmail(), user.getId());
            auditLogService.log(user.getId(), AuditLogService.ACTION_USER_LOGIN, ipAddress);
            return buildAuthResponse(user, true);

        } catch (BadCredentialsException ex) {
            // Track failed attempts and potentially lock account
            handleFailedLogin(user);
            auditLogService.log(user.getId(), AuditLogService.ACTION_USER_LOGIN_FAILED, ipAddress);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────

    /**
     * Rotate a refresh token and issue a new access token.
     *
     * <p>Token rotation security:
     * <ul>
     *   <li>Old token is revoked immediately.</li>
     *   <li>New token is issued in the same family.</li>
     *   <li>Reuse of old token triggers full family revocation.</li>
     * </ul>
     *
     * @param plainRefreshToken the plain refresh token from the client
     * @return a token-only AuthResponse (no user profile)
     */
    @Transactional
    public AuthResponse refresh(String plainRefreshToken) {
        // Validate the token, revoke it, and get the associated user
        RefreshToken oldToken = refreshTokenService.validateAndRotate(plainRefreshToken);
        User user = oldToken.getUser();

        // Ensure user is still active
        if (!user.isActive()) {
            throw new DisabledException("Account is deactivated.");
        }
        if (user.isLocked()) {
            throw new LockedException("Account is locked.");
        }

        // Issue new access token + new refresh token (same family = rotation chain)
        String newRefreshToken = refreshTokenService.createRefreshTokenInFamily(
                user, oldToken.getFamilyId());
        String newAccessToken = buildAccessToken(user);

        log.debug("Token refreshed for user {}", user.getId());
        return AuthResponse.tokenOnly(newAccessToken, newRefreshToken,
                jwtTokenUtil.getAccessTokenExpirationSeconds());
    }

    // ── Logout ────────────────────────────────────────────────────────────

    /**
     * Log out a user by revoking all their refresh tokens.
     *
     * @param userId the authenticated user's UUID
     */
    @Transactional
    public void logout(UUID userId) {
        logout(userId, null);
    }

    @Transactional
    public void logout(UUID userId, String ipAddress) {
        int revoked = refreshTokenService.revokeAllForUser(userId);
        log.info("User {} logged out. Revoked {} refresh token(s).", userId, revoked);
        auditLogService.log(userId, AuditLogService.ACTION_USER_LOGOUT, ipAddress);
    }

    // ── Current User ──────────────────────────────────────────────────────

    /**
     * Fetch the currently authenticated user's profile.
     *
     * @param userId the authenticated user's UUID (from SecurityContext)
     * @return the user's profile as a UserResponse DTO
     * @throws ResourceNotFoundException if the user no longer exists
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "Authenticated user not found"));
        return authMapper.toUserResponse(user);
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, boolean includeUser) {
        String accessToken = buildAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        long expiresIn = jwtTokenUtil.getAccessTokenExpirationSeconds();

        if (includeUser) {
            UserResponse userResponse = authMapper.toUserResponse(user);
            return AuthResponse.full(accessToken, refreshToken, expiresIn, userResponse);
        }
        return AuthResponse.tokenOnly(accessToken, refreshToken, expiresIn);
    }

    private String buildAccessToken(User user) {
        List<String> roles = List.of("ROLE_" + user.getRole().name());
        return jwtTokenUtil.generateAccessToken(
                buildUserDetails(user), user.getId(), user.getEmail(), roles);
    }

    private org.springframework.security.core.userdetails.UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }

    private void handleFailedLogin(User user) {
        user.incrementFailedAttempts();
        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLocked(true);
            log.warn("Account locked after {} failed attempts: {}", MAX_FAILED_ATTEMPTS, user.getEmail());
        }
        userRepository.save(user);
    }
}
