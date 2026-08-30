package com.commerceinsight.security;

import com.commerceinsight.admin.repository.AuditLogRepository;
import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.auth.domain.LoginHistory;
import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RefreshTokenRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.auth.repository.LoginHistoryRepository;
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

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LoginHistoryIntegrationTest — the orphaned {@code login_history} table is now
 * written on success and failure, and {@code audit_logs} records
 * {@code TOKEN_REFRESH}. Both writers are {@code @Async}, so assertions poll.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Login History & Audit Integration Tests")
@Sql(scripts = "/db/cleanup_security_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class LoginHistoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LoginHistoryRepository loginHistoryRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private static final String EMAIL = "loginhistory-user@example.com";
    private static final String PASSWORD = "LoginHist@123";

    @Test
    @DisplayName("records success + failure rows, and a TOKEN_REFRESH audit event")
    void writesLoginHistoryAndRefreshAudit() throws Exception {
        // register (also issues tokens)
        var reg = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Login", "History", EMAIL, PASSWORD))))
                .andExpect(status().isCreated())
                .andReturn();
        ApiResponse<AuthResponse> regBody = objectMapper.readValue(
                reg.getResponse().getContentAsString(), new TypeReference<>() {});
        String refreshToken = regBody.getData().getRefreshToken();

        // one successful login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, PASSWORD))))
                .andExpect(status().isOk());

        // one failed login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "WrongPass@999"))))
                .andExpect(status().isUnauthorized());

        // one refresh
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk());

        pollUntil("login_history has a success + an INVALID_CREDENTIALS failure for " + EMAIL, () -> {
            List<LoginHistory> rows = loginHistoryRepository.findAll().stream()
                    .filter(r -> EMAIL.equals(r.getEmail()))
                    .toList();
            return rows.stream().anyMatch(LoginHistory::isSuccess)
                    && rows.stream().anyMatch(r -> !r.isSuccess()
                            && "INVALID_CREDENTIALS".equals(r.getFailureReason()));
        });

        pollUntil("audit_logs has a TOKEN_REFRESH event", () ->
                auditLogRepository.findAll().stream()
                        .anyMatch(a -> AuditLogService.ACTION_TOKEN_REFRESH.equals(a.getAction())));
    }

    /** Poll a condition for up to ~10s — the audit / login-history writers are @Async. */
    private static void pollUntil(String description, BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for: " + description);
    }
}
