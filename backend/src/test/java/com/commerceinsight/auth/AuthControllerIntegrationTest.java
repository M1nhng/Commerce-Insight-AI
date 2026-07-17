package com.commerceinsight.auth;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RefreshTokenRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.shared.dto.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthControllerIntegrationTest — integration tests for the full auth flow.
 *
 * <p>Tests the complete HTTP request → Spring Security → Controller → Service → DB stack.
 * Uses @SpringBootTest (loads full application context) + MockMvc.
 *
 * <p>Database: Configured by application-test.yml (Testcontainers PostgreSQL).
 * Flyway applies all migrations before tests run.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>Register flow (success, duplicate email, invalid password)</li>
 *   <li>Login flow (success, wrong credentials, locked account)</li>
 *   <li>Token refresh flow (success, invalid token)</li>
 *   <li>Logout flow (success, unauthorized)</li>
 *   <li>Current user (GET /me) flow</li>
 *   <li>Protected endpoint authorization</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auth Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Shared state across ordered tests
    private static String accessToken;
    private static String refreshToken;
    private static final String TEST_EMAIL = "integration-test@example.com";
    private static final String TEST_PASSWORD = "IntegrationTest@123";

    // ── Register ─────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /auth/register — should register new user and return 201 with tokens")
    void register_success_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Integration", "User", TEST_EMAIL, TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.data.user.role").value("STAFF"))
                .andExpect(jsonPath("$.data.user.active").value(true))
                .andReturn();

        // Save tokens for subsequent tests
        String responseBody = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(responseBody,
                new TypeReference<>() {});
        accessToken = response.getData().getAccessToken();
        refreshToken = response.getData().getRefreshToken();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/register — should return 409 for duplicate email")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Dup", "User", TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/register — should return 400 for weak password")
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Weak", "Pass", "weak@example.com", "password"); // no uppercase, no special

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/register — should return 400 for invalid email")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Bad", "Email", "not-an-email", TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ── Login ──────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("POST /auth/login — should login successfully and return 200 with tokens")
    void login_success_returns200() throws Exception {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(TEST_EMAIL))
                .andReturn();

        // Update tokens
        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body, new TypeReference<>() {});
        accessToken = response.getData().getAccessToken();
        refreshToken = response.getData().getRefreshToken();
    }

    @Test
    @Order(6)
    @DisplayName("POST /auth/login — should return 401 for wrong password")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword@999");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("POST /auth/login — should return 401 for non-existent user")
    void login_nonExistentUser_returns401() throws Exception {
        LoginRequest request = new LoginRequest("nobody@example.com", TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── Token Refresh ──────────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("POST /auth/refresh — should issue new tokens with valid refresh token")
    void refresh_success_returns200() throws Exception {
        assertThat(refreshToken).as("Refresh token must be set from previous test").isNotBlank();

        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();

        // Update with new tokens
        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body, new TypeReference<>() {});
        String newAccessToken = response.getData().getAccessToken();
        String newRefreshToken = response.getData().getRefreshToken();

        assertThat(newAccessToken).isNotEqualTo(accessToken); // New access token
        assertThat(newRefreshToken).isNotEqualTo(refreshToken); // New refresh token (rotation)

        accessToken = newAccessToken;
        refreshToken = newRefreshToken;
    }

    @Test
    @Order(9)
    @DisplayName("POST /auth/refresh — should return 422 for invalid refresh token")
    void refresh_invalidToken_returns422() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("completely-invalid-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    @Order(10)
    @DisplayName("POST /auth/refresh — should detect token reuse and return 422")
    void refresh_tokenReuse_returns422() throws Exception {
        assertThat(refreshToken).as("Refresh token must be set").isNotBlank();

        // Use the refresh token once (valid)
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body, new TypeReference<>() {});
        accessToken = response.getData().getAccessToken();
        String newRefreshToken = response.getData().getRefreshToken();

        // Use the OLD refresh token again (reuse attack)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSE_DETECTED"));

        refreshToken = newRefreshToken;
    }

    // ── Current User ───────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("GET /auth/me — should return current user profile with valid JWT")
    void getCurrentUser_success_returns200() throws Exception {
        assertThat(accessToken).as("Access token must be set from previous test").isNotBlank();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @Order(12)
    @DisplayName("GET /auth/me — should return 401 without JWT")
    void getCurrentUser_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("GET /auth/me — should return 401 with invalid JWT")
    void getCurrentUser_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("POST /auth/logout — should return 204 and revoke tokens")
    void logout_success_returns204() throws Exception {
        assertThat(accessToken).as("Access token must be set").isNotBlank();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // After logout, refresh token should be revoked
        assertThat(refreshToken).isNotBlank();
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(15)
    @DisplayName("POST /auth/logout — should return 401 without JWT")
    void logout_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ── Verify Token ──────────────────────────────────────────────────────

    @Test
    @Order(16)
    @DisplayName("GET /auth/verify — should return 200 with user profile for valid JWT")
    void verifyToken_validToken_returns200() throws Exception {
        // Re-login to get a fresh token
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body, new TypeReference<>() {});
        String freshToken = response.getData().getAccessToken();

        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("Authorization", "Bearer " + freshToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    @Order(17)
    @DisplayName("GET /auth/verify — should return 401 with no token")
    void verifyToken_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(18)
    @DisplayName("GET /auth/verify — should return 401 with invalid JWT")
    void verifyToken_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("Authorization", "Bearer this.is.not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }
}
