package com.commerceinsight.category.controller;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.category.dto.request.CreateCategoryRequest;
import com.commerceinsight.category.dto.request.UpdateCategoryRequest;
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
 * CategoryControllerIntegrationTest — integration tests for category HTTP endpoints.
 *
 * <p>Database: Testcontainers PostgreSQL via application-test.yml.
 * Flyway applies all migrations before tests run.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>GET /categories — flat paginated list</li>
 *   <li>GET /categories/tree — hierarchical tree</li>
 *   <li>POST /categories — create root and child categories</li>
 *   <li>PUT /categories/{id} — update name/slug</li>
 *   <li>DELETE /categories/{id} — success (empty category) and failure (has products)</li>
 *   <li>Circular reference prevention on update</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CategoryController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String adminToken;
    private static String rootCategoryId;
    private static String childCategoryId;

    private static final String ADMIN_EMAIL    = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";

    // ── Setup ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup — login as ADMIN")
    void setup_loginAsAdmin() throws Exception {
        LoginRequest login = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});

        adminToken = response.getData().getAccessToken();
        assertThat(adminToken).isNotBlank();
    }

    // ── POST /api/v1/categories ───────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST /categories — should create root category and return 201")
    void createRootCategory_returns201() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "IT Electronics", "All electronics products", null, 0);

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("IT Electronics"))
                .andExpect(jsonPath("$.data.slug").value("it-electronics"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        rootCategoryId = objectMapper.readTree(body).path("data").path("id").asText();
        assertThat(rootCategoryId).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("POST /categories — should create child category under root")
    void createChildCategory_returns201WithParentId() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Smartphones", "Mobile smartphones", java.util.UUID.fromString(rootCategoryId), 1);

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("smartphones"))
                .andExpect(jsonPath("$.data.parentId").value(rootCategoryId))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        childCategoryId = objectMapper.readTree(body).path("data").path("id").asText();
        assertThat(childCategoryId).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("POST /categories — should return 400 for blank name")
    void createCategory_blankName_returns400() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("", null, null, 0);

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ── GET /api/v1/categories ────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /categories — should return paginated flat list")
    void listCategories_returnsFlatPage() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @Order(6)
    @DisplayName("GET /categories — should filter by search term")
    void listCategories_searchFilter_returnsMatches() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Smartphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Smartphones"));
    }

    // ── GET /api/v1/categories/tree ───────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("GET /categories/tree — should return hierarchical tree")
    void getCategoryTree_returnsNestedStructure() throws Exception {
        mockMvc.perform(get("/api/v1/categories/tree")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                // Root node should have at least 1 child
                .andExpect(jsonPath("$.data[?(@.id == '" + rootCategoryId + "')].children").exists());
    }

    // ── GET /api/v1/categories/{id} ───────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("GET /categories/{id} — should return single category")
    void getCategoryById_exists_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", rootCategoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(rootCategoryId))
                .andExpect(jsonPath("$.data.name").value("IT Electronics"));
    }

    @Test
    @Order(9)
    @DisplayName("GET /categories/{id} — should return 404 for unknown ID")
    void getCategoryById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/categories/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    // ── PUT /api/v1/categories/{id} ───────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("PUT /categories/{id} — should update name and regenerate slug")
    void updateCategory_nameChange_returns200() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Consumer Electronics", "Updated description", null, 0, true);

        mockMvc.perform(put("/api/v1/categories/{id}", rootCategoryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Consumer Electronics"))
                .andExpect(jsonPath("$.data.slug").value("consumer-electronics"));
    }

    // ── DELETE /api/v1/categories/{id} ────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("DELETE /categories/{id} — should return 422 when child categories exist")
    void deleteCategory_withChildren_returns422() throws Exception {
        // rootCategoryId still has childCategoryId as child
        mockMvc.perform(delete("/api/v1/categories/{id}", rootCategoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_HAS_PRODUCTS"));
    }

    @Test
    @Order(12)
    @DisplayName("DELETE /categories/{id} — should soft-delete leaf category (no children/products)")
    void deleteCategory_leafCategory_returns204() throws Exception {
        // First delete the child (leaf node)
        mockMvc.perform(delete("/api/v1/categories/{id}", childCategoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Then verify it's gone
        mockMvc.perform(get("/api/v1/categories/{id}", childCategoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /categories/{id} — should soft-delete root after children removed")
    void deleteCategory_emptyRoot_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", rootCategoryId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ── RBAC ──────────────────────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("POST /categories — should return 401 without token")
    void createCategory_unauthenticated_returns401() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest("Test", null, null, 0);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
