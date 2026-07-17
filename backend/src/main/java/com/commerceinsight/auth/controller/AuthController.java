package com.commerceinsight.auth.controller;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RefreshTokenRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.auth.service.AuthService;
import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AuthController — HTTP adapter for authentication endpoints.
 *
 * <p>Architecture Rules (STRICTLY ENFORCED):
 * <ul>
 *   <li>This controller contains ZERO business logic.</li>
 *   <li>It only: validates input (@Valid), delegates to AuthService, wraps response.</li>
 *   <li>It NEVER accesses any repository or domain entity directly.</li>
 *   <li>All responses use {@link ApiResponse} envelope.</li>
 * </ul>
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/auth/register — create a new STAFF account</li>
 *   <li>POST /api/v1/auth/login — authenticate and receive tokens</li>
 *   <li>POST /api/v1/auth/refresh — exchange refresh token for new access token</li>
 *   <li>POST /api/v1/auth/logout — revoke all refresh tokens</li>
 *   <li>GET  /api/v1/auth/me — get current user profile</li>
 *   <li>GET  /api/v1/auth/verify — verify token validity</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT authentication and user session management")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextHelper securityContextHelper;

    /**
     * POST /api/v1/auth/register
     *
     * <p>Register a new user account with STAFF role.
     * Returns HTTP 201 Created with access token, refresh token, and user profile.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user account",
               description = "Creates a new user account with STAFF role. Returns JWT tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, extractIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration successful"));
    }

    /**
     * POST /api/v1/auth/login
     *
     * <p>Authenticate with email and password.
     * Returns HTTP 200 OK with access token, refresh token, and user profile.
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password",
               description = "Authenticates user credentials and returns JWT access and refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, extractIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    /**
     * POST /api/v1/auth/refresh
     *
     * <p>Exchange a valid refresh token for a new access token + rotated refresh token.
     * The old refresh token is immediately revoked.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
               description = "Exchanges a refresh token for a new access token. Old refresh token is revoked (rotation).")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    /**
     * POST /api/v1/auth/logout
     *
     * <p>Revoke all refresh tokens for the current user.
     * Requires a valid JWT access token in Authorization header.
     * Returns HTTP 204 No Content.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Logout current user",
               description = "Revokes all refresh tokens for the authenticated user.")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        UUID currentUserId = securityContextHelper.getCurrentUserId();
        authService.logout(currentUserId, extractIp(httpRequest));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * GET /api/v1/auth/me
     *
     * <p>Fetch the currently authenticated user's profile.
     * Requires a valid JWT access token in Authorization header.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get current user profile",
               description = "Returns the profile of the currently authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UUID currentUserId = securityContextHelper.getCurrentUserId();
        UserResponse user = authService.getCurrentUser(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(user, "User profile retrieved successfully"));
    }

    /**
     * GET /api/v1/auth/verify
     *
     * <p>Verify that the current JWT access token is valid.
     * Returns 200 OK with user profile if valid; the JWT filter returns 401 automatically if invalid.
     * Useful for frontend token introspection on app startup.
     */
    @GetMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Verify token validity",
               description = "Returns 200 OK with user profile if the Bearer token is valid. " +
                             "Returns 401 Unauthorized if the token is missing, expired, or invalid.")
    public ResponseEntity<ApiResponse<UserResponse>> verifyToken() {
        UUID currentUserId = securityContextHelper.getCurrentUserId();
        UserResponse user = authService.getCurrentUser(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(user, "Token is valid"));
    }

    // ── Internal Helpers ──────────────────────────────────────────────────

    /**
     * Extract the client IP address from the request.
     * Handles reverse proxy via X-Forwarded-For header.
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
