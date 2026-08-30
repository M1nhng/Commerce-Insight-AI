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

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final LoginHistoryService loginHistoryService;
    private final LoginAttemptService loginAttemptService;

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
        return register(request, null, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        return register(request, ipAddress, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
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
        log.info("New user registered: id={}", savedUser.getId());

        auditLogService.log(savedUser.getId(), AuditLogService.ACTION_USER_CREATED,
                "User", savedUser.getId(), null, null, ipAddress, userAgent);

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
        return login(request, null, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        return login(request, ipAddress, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            loginHistoryService.record(email, null, ipAddress, userAgent,
                    false, LoginHistoryService.REASON_INVALID_CREDENTIALS);
            throw new BadCredentialsException("Invalid email or password");
        }

        // Pre-auth checks
        if (user.isLocked()) {
            loginHistoryService.record(email, user.getId(), ipAddress, userAgent,
                    false, LoginHistoryService.REASON_ACCOUNT_LOCKED);
            throw new LockedException("Account is locked. Please contact an administrator.");
        }
        if (!user.isActive()) {
            loginHistoryService.record(email, user.getId(), ipAddress, userAgent,
                    false, LoginHistoryService.REASON_ACCOUNT_DISABLED);
            throw new DisabledException("Account is deactivated.");
        }

        try {
            // Delegate to Spring Security (BCrypt verification + UserDetailsService)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getId().toString(),  // username = UUID (per UserDetailsServiceImpl)
                            request.password()
                    )
            );

            // Success — reset failed attempts, record login time
            user.resetFailedAttempts();
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            log.info("User logged in: id={}", user.getId());
            auditLogService.log(user.getId(), AuditLogService.ACTION_USER_LOGIN, ipAddress, userAgent);
            loginHistoryService.record(email, user.getId(), ipAddress, userAgent, true, null);
            return buildAuthResponse(user, true);

        } catch (BadCredentialsException ex) {
            // Persist the failed-attempt counter / lockout in its OWN transaction —
            // the rethrow below rolls THIS transaction back, so an in-tx write here
            // would be lost and the account would never actually lock.
            loginAttemptService.recordFailure(user.getId());
            auditLogService.log(user.getId(), AuditLogService.ACTION_USER_LOGIN_FAILED, ipAddress, userAgent);
            loginHistoryService.record(email, user.getId(), ipAddress, userAgent,
                    false, LoginHistoryService.REASON_INVALID_CREDENTIALS);
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
        return refresh(plainRefreshToken, null, null);
    }

    @Transactional
    public AuthResponse refresh(String plainRefreshToken, String ipAddress, String userAgent) {
        // Validate the token, revoke it, and get the associated user.
        // Reuse detection is audited inside RefreshTokenService.
        RefreshToken oldToken = refreshTokenService.validateAndRotate(plainRefreshToken, ipAddress);
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
        auditLogService.log(user.getId(), AuditLogService.ACTION_TOKEN_REFRESH, ipAddress, userAgent);
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
        logout(userId, null, null);
    }

    @Transactional
    public void logout(UUID userId, String ipAddress) {
        logout(userId, ipAddress, null);
    }

    @Transactional
    public void logout(UUID userId, String ipAddress, String userAgent) {
        int revoked = refreshTokenService.revokeAllForUser(userId);
        log.info("User {} logged out. Revoked {} refresh token(s).", userId, revoked);
        auditLogService.log(userId, AuditLogService.ACTION_USER_LOGOUT, ipAddress, userAgent);
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

}
