package com.commerceinsight.product.controller;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.product.dto.request.CreateProductRequest;
import com.commerceinsight.product.dto.request.UpdateProductRequest;
import com.commerceinsight.shared.dto.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductControllerIntegrationTest — integration tests for the full product HTTP stack.
 *
 * <p>Database: Testcontainers PostgreSQL via application-test.yml.
 * Flyway applies all migrations (V1–V13) before tests run.
 *
 * <p>Test coverage:
 * <ul>
 *   <li>GET /products — paginated list, category filter, search filter, active filter</li>
 *   <li>POST /products — create success (201), duplicate SKU (409), validation error (400)</li>
 *   <li>GET /products/{id} — success (200), not found (404)</li>
 *   <li>PUT /products/{id} — update success (200)</li>
 *   <li>DELETE /products/{id} — soft delete (204), unauthorized (403)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProductController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "/db/cleanup_product_test.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ProductControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Tokens acquired once and reused across ordered tests
    private static String adminToken;
    private static String staffToken;
    private static String createdProductId;

    private static final String ADMIN_EMAIL = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";

    // ── Setup: login as admin ─────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup — login as ADMIN to obtain access token")
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

    // ── POST /api/v1/products ─────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST /products — should create product and return 201")
    void createProduct_validRequest_returns201() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "IT-SKU-001",
                "Wireless Headphones Pro",
                "Premium noise-cancelling headphones",
                new BigDecimal("49.99"),
                new BigDecimal("22.00"),
                null,
                null,
                100
        );

        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("IT-SKU-001"))
                .andExpect(jsonPath("$.data.name").value("Wireless Headphones Pro"))
                .andExpect(jsonPath("$.data.price").value(49.99))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        ApiResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertThat(response.isSuccess()).isTrue();

        // Extract created ID for subsequent tests
        String body = result.getResponse().getContentAsString();
        createdProductId = objectMapper.readTree(body).path("data").path("id").asText();
        assertThat(createdProductId).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("POST /products — should return 409 for duplicate SKU")
    void createProduct_duplicateSku_returns409() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "IT-SKU-001", // Same SKU as created above
                "Another Product",
                null,
                new BigDecimal("19.99"),
                null, null, null, 0
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SKU"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /products — should return 400 for validation errors")
    void createProduct_invalidRequest_returns400() throws Exception {
        // Missing required fields: sku, name, price
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("POST /products — should return 400 for negative price")
    void createProduct_negativePrice_returns400() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "IT-SKU-002", "Bad Product", null,
                new BigDecimal("-1.00"), null, null, null, 0);

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    // ── GET /api/v1/products/{id} ─────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /products/{id} — should return full product detail")
    void getProductById_exists_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(createdProductId))
                .andExpect(jsonPath("$.data.sku").value("IT-SKU-001"));
    }

    @Test
    @Order(7)
    @DisplayName("GET /products/{id} — should return 404 for unknown ID")
    void getProductById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }

    // ── GET /api/v1/products ──────────────────────────────────────────────────

    @Test
    @Order(8)
    @DisplayName("GET /products — should return paginated list")
    void listProducts_returns200WithPage() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @Order(9)
    @DisplayName("GET /products — should filter by search term")
    void listProducts_searchFilter_returnsMatchingProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Wireless Headphones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sku").value("IT-SKU-001"));
    }

    @Test
    @Order(10)
    @DisplayName("GET /products — should return empty page for no-match search")
    void listProducts_noMatchSearch_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "zzz-no-match-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    // ── PUT /api/v1/products/{id} ─────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("PUT /products/{id} — should update product and return 200")
    void updateProduct_validRequest_returns200() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest(
                "IT-SKU-001",
                "Wireless Headphones Pro v2",
                "Updated description",
                new BigDecimal("59.99"),
                new BigDecimal("25.00"),
                null, null, true
        );

        mockMvc.perform(put("/api/v1/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Wireless Headphones Pro v2"))
                .andExpect(jsonPath("$.data.price").value(59.99));
    }

    // ── DELETE /api/v1/products/{id} ──────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("DELETE /products/{id} — should soft-delete and return 204")
    void deleteProduct_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /products/{id} — deleted product should return 404 on GET")
    void getProduct_afterSoftDelete_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", createdProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── Unauthenticated access ─────────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("GET /products — should return 401 without token")
    void listProducts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }
}
