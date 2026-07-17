package com.commerceinsight.user;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.request.RegisterRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.dto.request.ChangeRoleRequest;
import com.commerceinsight.user.dto.request.CreateUserRequest;
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
 * UserControllerIntegrationTest — integration tests for user management.
 *
 * <p>Tests the full HTTP → Security → Controller → Service → DB stack.
 * Validates RBAC: only ADMIN can access these endpoints.
 *
 * <p>Test strategy:
 * <ul>
 *   <li>Register a STAFF user via public endpoint.</li>
 *   <li>Seed an ADMIN user via DB (via the seeded admin from V11 migration).</li>
 *   <li>Verify STAFF gets 403, ADMIN gets 200.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Controller Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Shared state across ordered tests
    private static String adminToken;
    private static String staffToken;
    private static String createdUserId;

    private static final String ADMIN_EMAIL    = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String STAFF_EMAIL    = "userctrl-staff@example.com";
    private static final String STAFF_PASSWORD = "StaffPass@123";

    // ── Setup: Obtain tokens ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup: Login as seeded ADMIN user")
    void setup_loginAsAdmin() throws Exception {
        LoginRequest request = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body, new TypeReference<>() {});
        adminToken = response.getData().getAccessToken();

        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Setup: Register as STAFF user")
    void setup_registerAsStaff() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Staff", "User", STAFF_EMAIL, STAFF_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(body, new TypeReference<>() {});
        staffToken = response.getData().getAccessToken();

        assertThat(staffToken).isNotBlank();
    }

    // ── GET /api/v1/users ─────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("GET /users — should return 200 with user list for ADMIN")
    void listUsers_admin_returns200() throws Exception {
        assertThat(adminToken).as("Admin token must be set").isNotBlank();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    @Order(4)
    @DisplayName("GET /users — should return 403 for STAFF")
    void listUsers_staff_returns403() throws Exception {
        assertThat(staffToken).as("Staff token must be set").isNotBlank();

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(5)
    @DisplayName("GET /users — should return 401 without token")
    void listUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/v1/users ────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("POST /users — ADMIN should create a user and return 201")
    void createUser_admin_returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "New", "UserCtrl", "userctrl-new@example.com", "NewUser@123", Role.STAFF);

        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("userctrl-new@example.com"))
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andReturn();

        // Save created user ID for later tests
        String body = result.getResponse().getContentAsString();
        var node = objectMapper.readTree(body);
        createdUserId = node.get("data").get("id").asText();

        assertThat(createdUserId).isNotBlank();
    }

    @Test
    @Order(7)
    @DisplayName("POST /users — should return 409 for duplicate email")
    void createUser_duplicateEmail_returns409() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Dup", "User", "userctrl-new@example.com", "DupUser@123", Role.STAFF);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /users — STAFF should return 403")
    void createUser_staff_returns403() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Forbidden", "User", "forbidden@example.com", "ForbUser@123", Role.STAFF);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1/users/{id} ────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("GET /users/{id} — ADMIN should return 200 with user detail")
    void getUserById_admin_returns200() throws Exception {
        assertThat(createdUserId).as("Created user ID must be set").isNotBlank();

        mockMvc.perform(get("/api/v1/users/{id}", createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(createdUserId));
    }

    @Test
    @Order(10)
    @DisplayName("GET /users/{id} — should return 404 for non-existent user")
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    // ── PATCH /api/v1/users/{id}/role ─────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("PATCH /users/{id}/role — ADMIN should change user role")
    void changeRole_admin_returns200() throws Exception {
        assertThat(createdUserId).as("Created user ID must be set").isNotBlank();

        ChangeRoleRequest request = new ChangeRoleRequest(Role.MANAGER);

        mockMvc.perform(patch("/api/v1/users/{id}/role", createdUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("MANAGER"));
    }

    @Test
    @Order(12)
    @DisplayName("PATCH /users/{id}/role — should return 422 when ADMIN tries to change own role")
    void changeRole_selfChange_returns422() throws Exception {
        // Get the admin's own ID
        MvcResult me = mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        String body = me.getResponse().getContentAsString();
        String adminId = objectMapper.readTree(body).get("data").get("id").asText();

        ChangeRoleRequest request = new ChangeRoleRequest(Role.STAFF);

        mockMvc.perform(patch("/api/v1/users/{id}/role", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("USER_CANNOT_CHANGE_OWN_ROLE"));
    }

    // ── PATCH /api/v1/users/{id}/unlock ──────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("PATCH /users/{id}/unlock — should unlock a locked user")
    void unlockUser_admin_returns200() throws Exception {
        assertThat(createdUserId).as("Created user ID must be set").isNotBlank();

        mockMvc.perform(patch("/api/v1/users/{id}/unlock", createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locked").value(false));
    }

    // ── DELETE /api/v1/users/{id} ─────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("DELETE /users/{id} — ADMIN should soft-delete user and return 204")
    void deleteUser_admin_returns204() throws Exception {
        assertThat(createdUserId).as("Created user ID must be set").isNotBlank();

        mockMvc.perform(delete("/api/v1/users/{id}", createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // Verify user is no longer found (soft-deleted)
        mockMvc.perform(get("/api/v1/users/{id}", createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
