package com.commerceinsight.customer.controller;

import com.commerceinsight.auth.dto.request.LoginRequest;
import com.commerceinsight.auth.dto.response.AuthResponse;
import com.commerceinsight.customer.dto.request.*;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.domain.AddressType;
import com.commerceinsight.customer.domain.GroupStatus;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CustomerControllerIntegrationTest — full HTTP stack integration tests.
 *
 * <p>Covers:
 * <ul>
 *   <li>CustomerGroup CRUD (create, get, update, delete)</li>
 *   <li>Customer CRUD (create, get, update, soft-delete)</li>
 *   <li>Customer search by keyword, status, group</li>
 *   <li>Customer status update (ACTIVE → BLOCKED)</li>
 *   <li>Address management (add, update, delete, set-default)</li>
 *   <li>Default address uniqueness rule (only one default per type)</li>
 *   <li>Authorization: 401 for unauthenticated, 403 for wrong role</li>
 *   <li>Duplicate code/email → 409 Conflict</li>
 *   <li>Not found → 404</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CustomerController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "/db/cleanup_customer_test.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CustomerControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String adminToken;
    private static String staffToken;
    private static String createdGroupId;
    private static String createdCustomerId;
    private static String createdAddressId;

    private static final String ADMIN_EMAIL    = "admin@commerceinsight.ai";
    private static final String ADMIN_PASSWORD = "Admin@123456";
    private static final String STAFF_EMAIL    = "staff@commerceinsight.ai";
    private static final String STAFF_PASSWORD = "Staff@123456";

    // ── Setup ─────────────────────────────────────────────────────────────────

    @Test @Order(1) @DisplayName("Setup — login as ADMIN")
    void setup_loginAsAdmin() throws Exception {
        LoginRequest login = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        adminToken = response.getData().getAccessToken();
        assertThat(adminToken).isNotBlank();
    }

    // ── Customer Group ────────────────────────────────────────────────────────

    @Test @Order(10) @DisplayName("POST /customer-groups — 201 Created")
    void createGroup_returns201() throws Exception {
        CreateCustomerGroupRequest request = new CreateCustomerGroupRequest(
                "GRP-TEST-VIP", "VIP Test", "Test VIP group", GroupStatus.ACTIVE);

        MvcResult result = mockMvc.perform(post("/api/v1/customer-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("GRP-TEST-VIP"))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        createdGroupId = ((Map<?, ?>) body.get("data")).get("id").toString();
        assertThat(createdGroupId).isNotBlank();
    }

    @Test @Order(11) @DisplayName("POST /customer-groups — 409 Conflict on duplicate code")
    void createGroup_returns409_whenDuplicateCode() throws Exception {
        CreateCustomerGroupRequest request = new CreateCustomerGroupRequest(
                "GRP-TEST-VIP", "VIP Duplicate", null, null);

        mockMvc.perform(post("/api/v1/customer-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test @Order(12) @DisplayName("GET /customer-groups — 200 OK with list")
    void listGroups_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/customer-groups")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @Order(13) @DisplayName("GET /customer-groups/{id} — 200 OK")
    void getGroup_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/customer-groups/" + createdGroupId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("GRP-TEST-VIP"));
    }

    @Test @Order(14) @DisplayName("PUT /customer-groups/{id} — 200 OK update")
    void updateGroup_returns200() throws Exception {
        UpdateCustomerGroupRequest request = new UpdateCustomerGroupRequest(
                "VIP Test Updated", null, GroupStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/customer-groups/" + createdGroupId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("VIP Test Updated"));
    }

    // ── Customer CRUD ─────────────────────────────────────────────────────────

    @Test @Order(20) @DisplayName("POST /customers — 201 Created")
    void createCustomer_returns201() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-00001", "Jane", "Smith",
                "jane.smith@customer-test.com", "+84901234567",
                null, null, UUID.fromString(createdGroupId));

        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerCode").value("CUST-TEST-00001"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.groupName").value("VIP Test Updated"))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        createdCustomerId = ((Map<?, ?>) body.get("data")).get("id").toString();
        assertThat(createdCustomerId).isNotBlank();
    }

    @Test @Order(21) @DisplayName("POST /customers — 409 on duplicate code")
    void createCustomer_returns409_whenDuplicateCode() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-00001", "Other", "Person", null, null, null, null, null);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test @Order(22) @DisplayName("POST /customers — 409 on duplicate email")
    void createCustomer_returns409_whenDuplicateEmail() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-EMAIL-DUP", "Other", "Person",
                "jane.smith@customer-test.com", null, null, null, null);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test @Order(23) @DisplayName("POST /customers — 400 when firstName blank")
    void createCustomer_returns400_whenFirstNameBlank() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-99999", "", "Smith", null, null, null, null, null);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(24) @DisplayName("GET /customers/{id} — 200 OK")
    void getCustomer_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + createdCustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerCode").value("CUST-TEST-00001"))
                .andExpect(jsonPath("$.data.fullName").value("Jane Smith"));
    }

    @Test @Order(25) @DisplayName("GET /customers/{id} — 404 when not found")
    void getCustomer_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test @Order(26) @DisplayName("PUT /customers/{id} — 200 OK update")
    void updateCustomer_returns200() throws Exception {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Jane Updated", "Smith", null, null, null, null, null);

        mockMvc.perform(put("/api/v1/customers/" + createdCustomerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Jane Updated"));
    }

    // ── Customer Search / Filter ──────────────────────────────────────────────

    @Test @Order(27) @DisplayName("GET /customers?keyword=CUST-TEST — returns results")
    void searchCustomers_byKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .param("keyword", "CUST-TEST-00001")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test @Order(28) @DisplayName("GET /customers?status=ACTIVE — filters by status")
    void searchCustomers_byStatus() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test @Order(29) @DisplayName("GET /customers?groupId={id} — filters by group")
    void searchCustomers_byGroup() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .param("groupId", createdGroupId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ── Customer Status ───────────────────────────────────────────────────────

    @Test @Order(30) @DisplayName("PATCH /customers/{id}/status — 200 BLOCKED")
    void updateStatus_returnsBlocked() throws Exception {
        UpdateCustomerStatusRequest request = new UpdateCustomerStatusRequest(CustomerStatus.BLOCKED);

        mockMvc.perform(patch("/api/v1/customers/" + createdCustomerId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
    }

    @Test @Order(31) @DisplayName("PATCH /customers/{id}/status — restore to ACTIVE")
    void updateStatus_restoresActive() throws Exception {
        UpdateCustomerStatusRequest request = new UpdateCustomerStatusRequest(CustomerStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/customers/" + createdCustomerId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // ── Customer Addresses ────────────────────────────────────────────────────

    @Test @Order(40) @DisplayName("POST /customers/{id}/addresses — 201 Created")
    void addAddress_returns201() throws Exception {
        CreateAddressRequest request = new CreateAddressRequest(
                AddressType.SHIPPING, "Jane Smith", "+84901234567",
                "123 Main Street", "Ward 1", "District 1", "Ho Chi Minh City", "VN", true);

        MvcResult result = mockMvc.perform(post("/api/v1/customers/" + createdCustomerId + "/addresses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("SHIPPING"))
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        createdAddressId = ((Map<?, ?>) body.get("data")).get("id").toString();
        assertThat(createdAddressId).isNotBlank();
    }

    @Test @Order(41) @DisplayName("POST /customers/{id}/addresses — second default SHIPPING clears first")
    void addAddress_secondDefault_clearsFirst() throws Exception {
        // Sprint 14: was @Disabled — the final step GET-by-address-id hits a route
        // that only supports PUT/DELETE/PATCH (→ 405), yet asserted 404. There is
        // no single-address GET; verify the "only one default per type" rule via
        // the address LIST endpoint instead (which is what the old comment asked
        // for). Same intent, correct contract.
        CreateAddressRequest request = new CreateAddressRequest(
                AddressType.SHIPPING, "Jane Smith", null,
                "456 Second Street", null, "District 2", "Hanoi", "VN", true);

        mockMvc.perform(post("/api/v1/customers/" + createdCustomerId + "/addresses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        // The first SHIPPING address must no longer be the default.
        MvcResult listResult = mockMvc.perform(get("/api/v1/customers/" + createdCustomerId + "/addresses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        Map<String, Object> listBody = objectMapper.readValue(
                listResult.getResponse().getContentAsString(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> addresses = (List<Map<String, Object>>) listBody.get("data");

        Map<String, Object> firstAddress = addresses.stream()
                .filter(a -> createdAddressId.equals(String.valueOf(a.get("id"))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("first address " + createdAddressId + " missing from list"));
        assertThat(firstAddress.get("isDefault")).isEqualTo(false);

        long defaultShipping = addresses.stream()
                .filter(a -> "SHIPPING".equals(String.valueOf(a.get("type"))))
                .filter(a -> Boolean.TRUE.equals(a.get("isDefault")))
                .count();
        assertThat(defaultShipping).isEqualTo(1);
    }

    @Test @Order(42) @DisplayName("GET /customers/{id}/addresses — 200 OK list")
    void listAddresses_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + createdCustomerId + "/addresses")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test @Order(43) @DisplayName("PUT /customers/{id}/addresses/{addressId} — 200 OK update")
    void updateAddress_returns200() throws Exception {
        UpdateAddressRequest request = new UpdateAddressRequest(
                "Jane Updated", null, "789 Updated St", null, null, null, null);

        mockMvc.perform(put("/api/v1/customers/" + createdCustomerId + "/addresses/" + createdAddressId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientName").value("Jane Updated"));
    }

    @Test @Order(44) @DisplayName("PATCH /customers/{id}/addresses/{addressId}/default — 200 OK")
    void setDefault_returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/" + createdCustomerId + "/addresses/" + createdAddressId + "/default")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test @Order(45) @DisplayName("DELETE /customers/{id}/addresses/{addressId} — 204 No Content")
    void deleteAddress_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + createdCustomerId + "/addresses/" + createdAddressId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Test @Order(50) @DisplayName("GET /customers — 401 without token")
    void getCustomers_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(51) @DisplayName("DELETE /customers/{id} — 401 without token")
    void deleteCustomer_returns401_withoutToken() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + createdCustomerId))
                .andExpect(status().isUnauthorized());
    }

    // ── Soft Delete ───────────────────────────────────────────────────────────

    @Test @Order(60) @DisplayName("DELETE /customers/{id} — 204 soft delete")
    void deleteCustomer_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/" + createdCustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test @Order(61) @DisplayName("GET /customers/{id} — 404 after soft delete")
    void getCustomer_returns404_afterDelete() throws Exception {
        mockMvc.perform(get("/api/v1/customers/" + createdCustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── Group delete ──────────────────────────────────────────────────────────

    @Test @Order(70) @DisplayName("DELETE /customer-groups/{id} — 204 No Content")
    void deleteGroup_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/customer-groups/" + createdGroupId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }
}
