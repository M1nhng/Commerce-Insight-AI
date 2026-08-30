package com.commerceinsight.security;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.shared.dto.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SecurityHardeningIntegrationTest — Sprint 12A: enveloped 401/403, security
 * headers, request correlation, CORS, the locked-user token cut-off, and the
 * newly-protected import-templates endpoint.
 *
 * <p>Rate limiting is disabled for this class (test profile default).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Hardening Integration Tests")
@Sql(scripts = "/db/cleanup_security_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class SecurityHardeningIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String STAFF_EMAIL = "sec-hardening-staff@example.com";
    private static final String PASSWORD = "SecHarden@123";
    private static String staffToken;

    private String staffToken() throws Exception {
        if (staffToken != null) {
            return staffToken;
        }
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("Sec", "Staff", STAFF_EMAIL, PASSWORD))));
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(STAFF_EMAIL, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        staffToken = body.getData().getAccessToken();
        return staffToken;
    }

    // ── 401 / 403 envelopes ───────────────────────────────────────────────

    @Test
    @DisplayName("unauthenticated protected request → 401 with the standard envelope")
    void unauthenticated_returnsEnveloped401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.message", notNullValue()));
    }

    @Test
    @DisplayName("garbage Bearer token → 401 with the standard envelope")
    void garbageToken_returnsEnveloped401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("authenticated but wrong role → 403 with the standard envelope")
    void wrongRole_returnsEnveloped403() throws Exception {
        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // ── Headers / correlation ────────────────────────────────────────────

    @Test
    @DisplayName("security headers are present on API responses")
    void securityHeaders_present() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
                .andExpect(header().exists("Permissions-Policy"))
                .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("X-Request-Id is echoed on the response")
    void requestId_echoed() throws Exception {
        mockMvc.perform(get("/api/v1/products").header("X-Request-Id", "test-correlation-42"))
                .andExpect(header().string("X-Request-Id", "test-correlation-42"));
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(header().exists("X-Request-Id"));
    }

    // ── CORS ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CORS preflight from an allowed origin is accepted")
    void cors_allowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("CORS preflight from a disallowed origin is rejected")
    void cors_disallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    // ── F9: import templates now require auth ────────────────────────────

    @Test
    @DisplayName("GET /import/templates/{type} now requires authentication")
    void importTemplates_requiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/import/templates/PRODUCT"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/v1/import/templates/PRODUCT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken()))
                .andExpect(status().isOk());
    }

    // ── F6: a locked user's still-valid access token stops working ───────

    @Test
    @DisplayName("locking a user invalidates their live access token on the next request")
    void lockedUser_tokenRejected() throws Exception {
        String email = "sec-hardening-lockme@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("Lock", "Me", email, PASSWORD))));
        var login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> body = objectMapper.readValue(
                login.getResponse().getContentAsString(), new TypeReference<>() {});
        String token = body.getData().getAccessToken();

        // Token works now.
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        // Lock the account: 5 failed logins.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPass@999"))));
        }

        // Same still-unexpired token is now rejected.
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
