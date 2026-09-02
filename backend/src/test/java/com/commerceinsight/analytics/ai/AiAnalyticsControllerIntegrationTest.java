package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.llm.LlmCompletion;
import com.commerceinsight.analytics.ai.llm.LlmException;
import com.commerceinsight.analytics.ai.llm.LlmProvider;
import com.commerceinsight.analytics.ai.llm.LlmRequest;
import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.shared.dto.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP-stack integration test for {@code POST /api/v1/analytics/ai-insights}
 * against a real PostgreSQL, with the LLM provider replaced by a capturing stub
 * (no network, no API key, no real OpenAI/Claude/Gemini call — CI-safe).
 *
 * <p>Fixture: the Sprint 13D analytics seed ({@code seed_analytics_test.sql}) so
 * the AI context is built from real rows.
 *
 * <p>Verifies: structured 200 for an authenticated user (ADMIN + a freshly
 * registered STAFF, matching the existing "any authenticated role" analytics
 * policy); 401 without a token; 400 for an invalid date range; a provider
 * failure degrades to {@code available:false} with HTTP 200; and the prompt the
 * provider received contains real aggregates but no customer PII.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AiAnalyticsControllerIntegrationTest.StubLlmConfig.class)
@TestPropertySource(properties = {
        "app.ai.enabled=true",
        "app.ai.provider=stub",
        "app.rate-limit.enabled=false"
})
@DisplayName("AiAnalyticsController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {"/db/cleanup_ai_analytics_test.sql", "/db/cleanup_analytics_test.sql", "/db/seed_analytics_test.sql"},
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = {"/db/cleanup_analytics_test.sql", "/db/cleanup_ai_analytics_test.sql"},
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class AiAnalyticsControllerIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String STAFF_EMAIL = "ai-insights-staff@example.com";
    private static final String STAFF_PASSWORD = "StaffPass#123";

    private static final String BODY = """
            {"dateFrom":"2025-01-01T00:00:00Z","dateTo":"2026-01-01T00:00:00Z"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CapturingStubProvider stub;

    private static String adminToken;
    private static String staffToken;

    // ── Stub provider ────────────────────────────────────────────────────────

    static class CapturingStubProvider implements LlmProvider {
        final AtomicReference<LlmRequest> lastRequest = new AtomicReference<>();
        volatile boolean fail = false;

        @Override public String id() { return "stub"; }
        @Override public boolean isConfigured() { return true; }

        @Override public LlmCompletion complete(LlmRequest request) {
            lastRequest.set(request);
            if (fail) {
                throw new LlmException("stub provider failure");
            }
            String json = """
                    {"summary":"Revenue for the window is summarised from the supplied aggregates.",
                     "insights":[{"type":"TREND","title":"Revenue trend","description":"Monthly revenue points were provided.","metric":"","severity":"LOW"}],
                     "recommendations":[{"title":"Watch cancellations","description":"Cancelled orders are present in the data.","priority":"MEDIUM"}]}""";
            return new LlmCompletion(json, "stub", "stub-model");
        }
    }

    @TestConfiguration
    static class StubLlmConfig {
        @Bean @Primary
        CapturingStubProvider capturingStubProvider() {
            return new CapturingStubProvider();
        }
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    @Test @Order(1) @DisplayName("Setup — tokens for ADMIN and a new STAFF user")
    void setup() throws Exception {
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Register a STAFF user (register defaults to STAFF), then log in.
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "Ai", "Staff", STAFF_EMAIL, STAFF_PASSWORD))))
                .andExpect(status().isCreated());
        staffToken = login(STAFF_EMAIL, STAFF_PASSWORD);

        stub.fail = false;
        assertThat(adminToken).isNotBlank();
        assertThat(staffToken).isNotBlank();
    }

    @Test @Order(10) @DisplayName("ADMIN → 200 with a structured, available response")
    void admin_getsStructuredInsights() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/ai-insights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.summary").isNotEmpty())
                .andExpect(jsonPath("$.data.insights").isArray())
                .andExpect(jsonPath("$.data.recommendations").isArray())
                .andExpect(jsonPath("$.data.provider").value("stub"))
                .andExpect(jsonPath("$.data.model").value("stub-model"));
    }

    @Test @Order(11) @DisplayName("STAFF → 200 (existing analytics policy admits any authenticated role)")
    void staff_allowed() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/ai-insights")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test @Order(20) @DisplayName("no token → 401 (unchanged auth boundary)")
    void unauthenticated_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/ai-insights")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(30) @DisplayName("dateFrom after dateTo → 400 with a safe envelope")
    void invalidRange_rejected() throws Exception {
        String bad = "{\"dateFrom\":\"2026-06-01T00:00:00Z\",\"dateTo\":\"2026-01-01T00:00:00Z\"}";
        mockMvc.perform(post("/api/v1/analytics/ai-insights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test @Order(31) @DisplayName("missing dateTo → 400 (bean validation)")
    void missingBound_rejected() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/ai-insights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateFrom\":\"2026-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test @Order(40) @DisplayName("provider failure → HTTP 200 with available:false (dashboard unaffected)")
    void providerFailure_isSafe() throws Exception {
        stub.fail = true;
        try {
            mockMvc.perform(post("/api/v1/analytics/ai-insights")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(false))
                    .andExpect(jsonPath("$.data.insights").isEmpty())
                    .andExpect(jsonPath("$.data.provider").doesNotExist());
        } finally {
            stub.fail = false;
        }
    }

    @Test @Order(50) @DisplayName("the prompt sent to the provider has real aggregates and no customer PII")
    void promptIsSafeAndGrounded() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/ai-insights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());

        LlmRequest sent = stub.lastRequest.get();
        assertThat(sent).isNotNull();
        String user = sent.userPrompt();
        String all = (sent.systemPrompt() + "\n" + user).toLowerCase();

        // Grounded: the compact analytics context is embedded.
        assertThat(user).contains("DATA CONTEXT");
        assertThat(user).contains("\"currency\"");
        assertThat(user).contains("\"revenueByMonth\"");
        assertThat(user).contains("\"ordersByStatus\"");

        // Safe: no PII, no secrets, no auth material.
        assertThat(all).doesNotContain("@example.com");
        assertThat(all).doesNotContain("password");
        assertThat(all).doesNotContain("bearer ");
        assertThat(all).doesNotContain("authorization:");
        assertThat(all).doesNotContain("x-mcp-api-key");
        assertThat(all).doesNotContain("\"email\"");
        assertThat(all).doesNotContain("\"phone\"");
        assertThat(all).doesNotContain("jdbc:");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String login(String email, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> body = objectMapper.readValue(
                res.getResponse().getContentAsString(), new TypeReference<>() {});
        return body.getData().getAccessToken();
    }
}
