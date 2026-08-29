package com.commerceinsight.export.service;

import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;
import com.commerceinsight.order.dto.response.OrderSummaryResponse;
import com.commerceinsight.order.service.OrderService;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExportService")
class OrderExportServiceTest {

    @Mock private OrderService orderService;
    private OrderExportService service;

    private static final Instant CREATED = Instant.parse("2026-08-20T10:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-08-21T11:00:00Z");

    @BeforeEach
    void setUp() {
        service = new OrderExportService(orderService, new ExportProperties());
    }

    private OrderSummaryResponse order(String number, int itemCount, BigDecimal total) {
        return new OrderSummaryResponse(UUID.randomUUID(), number, UUID.randomUUID(), "Jane Buyer",
                OrderStatus.COMPLETED, PaymentStatus.PAID, total, "VND", itemCount, CREATED, UPDATED);
    }

    private PageResponse<OrderSummaryResponse> page(List<OrderSummaryResponse> content, long total) {
        return PageResponse.from(new PageImpl<>(content, PageRequest.of(0, 1000), total));
    }

    @Test
    @DisplayName("emits exactly one row per order even when an order has many items")
    void oneRowPerOrder() {
        when(orderService.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(
                        order("ORD-1", 3, new BigDecimal("300.00")),
                        order("ORD-2", 1, new BigDecimal("50.00"))), 2));

        ReportDocument doc = service.buildReport(null, null, null, null, null, null);
        ReportTable table = doc.tables().get(0);

        assertThat(table.name()).isEqualTo("Orders");
        assertThat(table.rows()).hasSize(2);
        assertThat(table.columns()).extracting(c -> c.header())
                .containsExactly("Order Number", "Customer", "Status", "Payment Status", "Currency",
                        "Items", "Total", "Created At (UTC)", "Updated At (UTC)");
        assertThat(table.rows().get(0)).containsExactly(
                "ORD-1", "Jane Buyer", "COMPLETED", "PAID", "VND", 3, new BigDecimal("300.00"), CREATED, UPDATED);
    }

    @Test
    @DisplayName("takes the order total verbatim from the DTO (no recomputation)")
    void totalIsVerbatim() {
        BigDecimal exact = new BigDecimal("1234.5678");
        when(orderService.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(order("ORD-9", 7, exact)), 1));

        ReportDocument doc = service.buildReport(null, null, null, null, null, null);

        assertThat(doc.tables().get(0).rows().get(0).get(6)).isEqualTo(exact);
    }

    @Test
    @DisplayName("forwards keyword / customerId / status / paymentStatus / date bounds to OrderService.findAll")
    void forwardsFilters() {
        when(orderService.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(), 0));
        UUID customerId = UUID.randomUUID();
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");

        service.buildReport("ORD", customerId, OrderStatus.COMPLETED, PaymentStatus.PAID, from, to);

        verify(orderService).findAll(eq("ORD"), eq(customerId), eq(OrderStatus.COMPLETED),
                eq(PaymentStatus.PAID), eq(from), eq(to), any(Pageable.class));
    }

    @Test
    @DisplayName("rejects with EXPORT_ROW_LIMIT_EXCEEDED before loading a second page")
    void rowLimitExceeded() {
        ExportProperties tiny = new ExportProperties();
        tiny.setMaxRows(5);
        OrderExportService limited = new OrderExportService(orderService, tiny);
        // page size == content size so PageImpl keeps the large totalElements we pass
        when(orderService.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(PageResponse.from(new PageImpl<>(
                        List.of(order("ORD-1", 1, BigDecimal.TEN)), PageRequest.of(0, 1), 6)));

        assertThatThrownBy(() -> limited.buildReport(null, null, null, null, null, null))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> assertThat(((ExportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXPORT_ROW_LIMIT_EXCEEDED));

        verify(orderService, times(1)).findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }
}
