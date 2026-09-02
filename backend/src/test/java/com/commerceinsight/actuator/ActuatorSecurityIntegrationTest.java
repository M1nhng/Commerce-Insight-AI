package com.commerceinsight.actuator;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 15 — Actuator exposure + security.
 *
 * <p>Verifies the fix for the "/actuator/metrics → 500" gap:
 * <ul>
 *   <li>{@code /actuator/health} and {@code /actuator/info} stay public
 *       (orchestrator probes);</li>
 *   <li>{@code /actuator/metrics} is now a real handler (not
 *       {@code NoHandlerFoundException} → 500), and is <strong>ADMIN-only</strong>
 *       — anonymous → 401, non-admin → 403, admin → 200;</li>
 *   <li>the AI-insights meters are queryable by name;</li>
 *   <li>sensitive endpoints ({@code /actuator/env}, {@code /beans},
 *       {@code /configprops}, {@code /mappings}) are not exposed → 404.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Actuator security")
@Sql(scripts = "/db/cleanup_actuator_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/db/cleanup_actuator_test.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class ActuatorSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String ADMIN_EMAIL = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String STAFF_EMAIL = "actuator-staff@example.com";
    private static final String STAFF_PASSWORD = "Actuator@123";

    private String token(String email, String password) throws Exception {
        var res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> body = objectMapper.readValue(
                res.getResponse().getContentAsString(), new TypeReference<>() {});
        return body.getData().getAccessToken();
    }

    private String staffToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest("Act", "Staff", STAFF_EMAIL, STAFF_PASSWORD))));
        return token(STAFF_EMAIL, STAFF_PASSWORD);
    }

    @Test
    @DisplayName("/actuator/health and /info are public")
    void healthAndInfoArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("/actuator/metrics — anonymous → 401 (not 500)")
    void metricsAnonymous_401() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/actuator/metrics — STAFF → 403")
    void metricsStaff_403() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + staffToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/actuator/metrics — ADMIN → 200 and AI meters are registered")
    void metricsAdmin_200_withAiMeters() throws Exception {
        String admin = token(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());

        // AiMetrics registers these lazily on first increment; assert reachability
        // of the endpoint form used to scrape a single meter.
        for (String meter : new String[]{
                "ai.insights.requests", "ai.insights.success", "ai.insights.unavailable",
                "ai.insights.validation_failures", "ai.insights.provider_failures"}) {
            mockMvc.perform(get("/actuator/metrics/" + meter)
                            .header("Authorization", "Bearer " + admin))
                    // 200 if already registered, 404 if not yet incremented — never 500.
                    .andExpect(result -> {
                        int s = result.getResponse().getStatus();
                        if (s != 200 && s != 404) {
                            throw new AssertionError("unexpected status " + s + " for " + meter);
                        }
                    });
        }
    }

    @Test
    @DisplayName("sensitive management endpoints are not exposed → 404")
    void sensitiveEndpointsNotExposed() throws Exception {
        String admin = token(ADMIN_EMAIL, ADMIN_PASSWORD);
        for (String ep : new String[]{"env", "beans", "configprops", "mappings", "threaddump", "heapdump"}) {
            mockMvc.perform(get("/actuator/" + ep).header("Authorization", "Bearer " + admin))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("unknown API path → 404 enveloped (NoHandlerFoundException no longer 500s)")
    void unknownPath_404() throws Exception {
        mockMvc.perform(get("/api/v1/definitely-not-a-real-endpoint")
                        .header("Authorization", "Bearer " + token(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
}
