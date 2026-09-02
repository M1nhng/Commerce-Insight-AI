package com.commerceinsight.analytics.controller;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.shared.dto.ApiResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AnalyticsControllerIntegrationTest — full HTTP stack for /api/v1/analytics/*
 * against a real PostgreSQL (Testcontainers via application-test.yml).
 *
 * <p>Sprint 13D: proves the NULL-bind fix in {@code AnalyticsRepository}
 * (nullable date parameters wrapped in {@code CAST(:p AS timestamptz)}). Before
 * the fix every one of these endpoints returned HTTP 500
 * ({@code ERROR: could not determine data type of parameter $1}).
 *
 * <p>Fixture: {@code /db/seed_analytics_test.sql} inserts 12 orders across 3
 * distinct calendar months (6 revenue-eligible, 3 CANCELLED, 3 PENDING) with
 * order items and payments. Other integration-test classes may leave their own
 * {@code *-TEST-*} rows behind, so assertions use lower bounds / non-zero checks,
 * never exact totals — except the empty-future-range case which must be all-zero.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AnalyticsController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {"/db/cleanup_analytics_test.sql", "/db/seed_analytics_test.sql"},
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/db/cleanup_analytics_test.sql",
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class AnalyticsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String adminToken;

    private static final String ADMIN_EMAIL    = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";

    /** GET an analytics endpoint, assert HTTP 200 + enveloped success, return the {@code data} node. */
    private JsonNode getData(String path) throws Exception {
        MvcResult res = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Setup — acquire admin token")
    void setup_login() throws Exception {
        LoginRequest login = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthResponse> resp = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        adminToken = resp.getData().getAccessToken();
        assertThat(adminToken).isNotBlank();
    }

    // ── A. Overview ──────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("GET /analytics/overview — 200 with non-zero KPIs")
    void overview_returnsNonZeroData() throws Exception {
        JsonNode d = getData("/api/v1/analytics/overview");
        assertThat(d.get("totalRevenue").decimalValue().signum()).isPositive();
        assertThat(d.get("totalOrders").asLong()).isGreaterThanOrEqualTo(12);
        assertThat(d.get("totalCustomers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("totalProductsSold").asLong()).isPositive();
        assertThat(d.get("cancelledOrders").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(d.get("cancellationRate").decimalValue().signum()).isGreaterThanOrEqualTo(0);
        assertThat(d.get("currency").asText()).isEqualTo("VND");
    }

    // ── B. Revenue ───────────────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("GET /analytics/revenue?groupBy=MONTH — 200 with >= 2 monthly points, non-negative")
    void revenue_monthly_hasMultiplePoints() throws Exception {
        JsonNode d = getData("/api/v1/analytics/revenue?groupBy=MONTH");
        assertThat(d.get("groupBy").asText()).isEqualTo("MONTH");
        JsonNode points = d.get("data");
        assertThat(points.isArray()).isTrue();
        assertThat(points.size()).isGreaterThanOrEqualTo(2);
        for (JsonNode p : points) {
            assertThat(p.get("period").asText()).matches("\\d{4}-\\d{2}");
            assertThat(p.get("revenue").decimalValue().signum()).isGreaterThanOrEqualTo(0);
            assertThat(p.get("orders").asLong()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @Order(21)
    @DisplayName("GET /analytics/revenue?groupBy=DAY — 200 (no PostgreSQL type error)")
    void revenue_daily_ok() throws Exception {
        assertThat(getData("/api/v1/analytics/revenue?groupBy=DAY").get("data").isArray()).isTrue();
    }

    // ── C. Orders ────────────────────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("GET /analytics/orders — 200 with status distribution, consistent totals")
    void orders_statusDistribution() throws Exception {
        JsonNode d = getData("/api/v1/analytics/orders");
        long total = d.get("totalOrders").asLong();
        long sum = d.get("pendingOrders").asLong() + d.get("confirmedOrders").asLong()
                + d.get("processingOrders").asLong() + d.get("shippedOrders").asLong()
                + d.get("deliveredOrders").asLong() + d.get("completedOrders").asLong()
                + d.get("cancelledOrders").asLong();
        assertThat(total).isGreaterThanOrEqualTo(12);
        // total includes REFUNDED (not broken out) so sum <= total
        assertThat(sum).isLessThanOrEqualTo(total);
        assertThat(d.get("completedOrders").asLong()).isGreaterThanOrEqualTo(4);
        assertThat(d.get("cancelledOrders").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(d.get("completionRate").decimalValue().doubleValue()).isBetween(0.0, 100.0);
        assertThat(d.get("cancellationRate").decimalValue().doubleValue()).isBetween(0.0, 100.0);
    }

    // ── D. Top products ──────────────────────────────────────────────────────

    @Test
    @Order(40)
    @DisplayName("GET /analytics/products/top — 200 with at least one named product")
    void topProducts_hasEntries() throws Exception {
        JsonNode arr = getData("/api/v1/analytics/products/top?limit=5");
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);
        JsonNode first = arr.get(0);
        assertThat(first.get("productName").asText()).isNotBlank();
        assertThat(first.get("quantitySold").asLong()).isPositive();
        assertThat(first.get("revenue").decimalValue().signum()).isGreaterThanOrEqualTo(0);
    }

    // ── E. Customers ─────────────────────────────────────────────────────────

    @Test
    @Order(50)
    @DisplayName("GET /analytics/customers — 200 with non-empty customer analytics")
    void customerAnalytics_nonEmpty() throws Exception {
        JsonNode d = getData("/api/v1/analytics/customers");
        assertThat(d.get("uniqueCustomers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("newCustomers").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(d.get("repeatCustomers").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(d.get("averageOrdersPerCustomer").decimalValue().signum()).isGreaterThanOrEqualTo(0);
    }

    // ── F. Payments ──────────────────────────────────────────────────────────

    @Test
    @Order(60)
    @DisplayName("GET /analytics/payments — 200 with method distribution, non-negative amounts")
    void paymentAnalytics_breakdown() throws Exception {
        JsonNode d = getData("/api/v1/analytics/payments");
        assertThat(d.get("currency").asText()).isEqualTo("VND");
        JsonNode breakdown = d.get("breakdown");
        assertThat(breakdown.size()).isGreaterThanOrEqualTo(1);
        breakdown.forEach(v -> {
            assertThat(v.get("orders").asLong()).isGreaterThanOrEqualTo(0);
            assertThat(v.get("amount").decimalValue().signum()).isGreaterThanOrEqualTo(0);
        });
    }

    // ── G. Date filtering (the actual regression under test) ─────────────────

    @Test
    @Order(70)
    @DisplayName("Date parameters no longer cause a PostgreSQL type error")
    void dateFilterPermutations_allReturn200() throws Exception {
        String wide = "dateFrom=2000-01-01T00:00:00Z&dateTo=2100-01-01T00:00:00Z";
        // every endpoint × {no filter, from only, to only, from+to}
        for (String ep : new String[]{
                "/api/v1/analytics/overview",
                "/api/v1/analytics/revenue?groupBy=MONTH",
                "/api/v1/analytics/orders",
                "/api/v1/analytics/products/top?limit=3",
                "/api/v1/analytics/customers",
                "/api/v1/analytics/payments"}) {
            String sep = ep.contains("?") ? "&" : "?";
            getData(ep);
            getData(ep + sep + "dateFrom=2000-01-01T00:00:00Z");
            getData(ep + sep + "dateTo=2100-01-01T00:00:00Z");
            getData(ep + sep + wide);
        }
    }

    // ── H. Empty date range — existing contract: 200 + zero/empty ────────────

    @Test
    @Order(80)
    @DisplayName("Empty (future) date range — 200 with zero/empty results")
    void emptyDateRange_returnsZeroes() throws Exception {
        String future = "?dateFrom=2099-01-01T00:00:00Z&dateTo=2099-12-31T00:00:00Z";
        JsonNode ov = getData("/api/v1/analytics/overview" + future);
        assertThat(ov.get("totalRevenue").decimalValue().signum()).isZero();
        assertThat(ov.get("totalOrders").asLong()).isZero();
        assertThat(ov.get("totalCustomers").asLong()).isZero();
        assertThat(ov.get("totalProductsSold").asLong()).isZero();

        JsonNode rev = getData("/api/v1/analytics/revenue?groupBy=MONTH&dateFrom=2099-01-01T00:00:00Z");
        assertThat(rev.get("data").isArray()).isTrue();
        assertThat(rev.get("data").size()).isZero();

        JsonNode pay = getData("/api/v1/analytics/payments" + future);
        assertThat(pay.get("breakdown").size()).isZero();
    }

    // ── Security — analytics still requires authentication ───────────────────

    @Test
    @Order(90)
    @DisplayName("GET /analytics/overview without a token — 401")
    void overview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
