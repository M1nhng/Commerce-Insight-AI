package com.commerceinsight.export.service;

import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.dto.response.CustomerSummaryResponse;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.shared.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerExportService")
class CustomerExportServiceTest {

    @Mock private CustomerService customerService;
    private CustomerExportService service;

    private static final Instant CREATED = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        service = new CustomerExportService(customerService, new ExportProperties());
    }

    private CustomerSummaryResponse customer(String code, String first, String last, String email) {
        return new CustomerSummaryResponse(UUID.randomUUID(), code, first, last, first + " " + last,
                email, "0900" + code, CustomerStatus.ACTIVE, UUID.randomUUID(), "VIP", CREATED);
    }

    private PageResponse<CustomerSummaryResponse> page(List<CustomerSummaryResponse> content, long total) {
        return PageResponse.from(new PageImpl<>(content, PageRequest.of(0, 1000), total));
    }

    @Test
    @DisplayName("maps the customer summary read model into a 'Customers' table")
    void mapsRows() {
        when(customerService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(customer("C1", "Ada", "Lovelace", "ada@x.io")), 1));

        ReportDocument doc = service.buildReport(null, null, null, null, null);
        ReportTable table = doc.tables().get(0);

        assertThat(doc.title()).isEqualTo("Customers Export");
        assertThat(table.name()).isEqualTo("Customers");
        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().get(0)).containsExactly(
                "C1", "Ada", "Lovelace", "Ada Lovelace", "ada@x.io", "0900C1", "ACTIVE", "VIP", CREATED);
    }

    @Test
    @DisplayName("never exposes password / token / credential columns")
    void noSensitiveColumns() {
        when(customerService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(customer("C1", "Ada", "Lovelace", "ada@x.io")), 1));

        ReportDocument doc = service.buildReport(null, null, null, null, null);

        List<String> headers = doc.tables().get(0).columns().stream().map(ReportColumn::header).toList();
        assertThat(headers).noneSatisfy(h -> assertThat(h.toLowerCase())
                .containsAnyOf("password", "hash", "token", "jwt", "secret", "credential"));
    }

    @Test
    @DisplayName("forwards keyword / status / groupId / date bounds to CustomerService.findAll")
    void forwardsFilters() {
        when(customerService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(), 0));
        UUID groupId = UUID.randomUUID();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T00:00:00Z");

        service.buildReport("ada", CustomerStatus.BLOCKED, groupId, from, to);

        verify(customerService).findAll(eq("ada"), eq(CustomerStatus.BLOCKED), eq(groupId),
                eq(from), eq(to), any(Pageable.class));
    }

    @Test
    @DisplayName("an empty result set yields a valid zero-row report")
    void emptyResult() {
        when(customerService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(), 0));

        assertThat(service.buildReport(null, null, null, null, null).tables().get(0).rows()).isEmpty();
    }
}
