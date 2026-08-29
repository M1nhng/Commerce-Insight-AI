package com.commerceinsight.export.service;

import com.commerceinsight.analytics.dto.CustomerAnalyticsResponse;
import com.commerceinsight.analytics.dto.OrderAnalyticsResponse;
import com.commerceinsight.analytics.dto.PaymentAnalyticsResponse;
import com.commerceinsight.analytics.dto.PaymentMethodStats;
import com.commerceinsight.analytics.dto.RevenuePeriodResponse;
import com.commerceinsight.analytics.dto.RevenueResponse;
import com.commerceinsight.analytics.dto.TopProductEntry;
import com.commerceinsight.analytics.service.AnalyticsService;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsExportService")
class AnalyticsExportServiceTest {

    @Mock private AnalyticsService analyticsService;
    @InjectMocks private AnalyticsExportService service;

    private static final Instant FROM = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-08-31T23:59:59Z");

    @Test
    @DisplayName("revenue: delegates to AnalyticsService.getRevenue and maps the time series verbatim")
    void revenue() {
        when(analyticsService.getRevenue(FROM, TO, "MONTH")).thenReturn(new RevenueResponse(
                "MONTH", "VND", FROM, TO, List.of(
                new RevenuePeriodResponse("2026-08", new BigDecimal("1000.00"), 12L),
                new RevenuePeriodResponse("2026-09", new BigDecimal("2000.00"), 20L))));

        ReportDocument doc = service.buildRevenueReport(FROM, TO, "MONTH");
        ReportTable table = doc.tables().get(0);

        assertThat(doc.title()).contains("groupBy=MONTH");
        assertThat(table.columns()).extracting(c -> c.header())
                .containsExactly("Period", "Revenue", "Orders", "Currency");
        assertThat(table.rows()).hasSize(2);
        assertThat(table.rows().get(0)).containsExactly("2026-08", new BigDecimal("1000.00"), 12L, "VND");
        verify(analyticsService, times(1)).getRevenue(FROM, TO, "MONTH");
        verifyNoMoreInteractions(analyticsService);
    }

    @Test
    @DisplayName("order analytics: single wide row of counts + rates straight from the DTO")
    void orderAnalytics() {
        when(analyticsService.getOrderAnalytics(FROM, TO)).thenReturn(new OrderAnalyticsResponse(
                100, 10, 20, 5, 8, 12, 40, 5,
                new BigDecimal("40.00"), new BigDecimal("5.00"), FROM, TO));

        ReportTable table = service.buildOrderAnalyticsReport(FROM, TO).tables().get(0);

        assertThat(table.rows()).hasSize(1);
        assertThat(table.rows().get(0)).containsExactly(
                100L, 10L, 20L, 5L, 8L, 12L, 40L, 5L, new BigDecimal("40.00"), new BigDecimal("5.00"));
        verify(analyticsService).getOrderAnalytics(FROM, TO);
    }

    @Test
    @DisplayName("top products: rows preserve the service ordering — no local sort/aggregate")
    void topProducts() {
        UUID id = UUID.randomUUID();
        when(analyticsService.getTopProducts(FROM, TO, 3)).thenReturn(List.of(
                new TopProductEntry(id, "SKU-B", "B", 5L, new BigDecimal("500")),
                new TopProductEntry(null, "SKU-A", "A", 9L, new BigDecimal("100")),
                new TopProductEntry(UUID.randomUUID(), "SKU-C", "C", 1L, new BigDecimal("999"))));

        ReportTable table = service.buildTopProductsReport(FROM, TO, 3).tables().get(0);

        assertThat(table.rows()).extracting(r -> r.get(1)).containsExactly("SKU-B", "SKU-A", "SKU-C");
        assertThat(table.rows().get(0).get(0)).isEqualTo(id.toString());
        assertThat(table.rows().get(1).get(0)).isEqualTo("");
        verify(analyticsService).getTopProducts(FROM, TO, 3);
    }

    @Test
    @DisplayName("customer analytics: single row of engagement metrics from the DTO")
    void customerAnalytics() {
        when(analyticsService.getCustomerAnalytics(FROM, TO)).thenReturn(new CustomerAnalyticsResponse(
                50, 15, 20, new BigDecimal("2.35"), FROM, TO));

        ReportTable table = service.buildCustomerAnalyticsReport(FROM, TO).tables().get(0);

        assertThat(table.columns()).extracting(c -> c.header())
                .containsExactly("Unique Customers", "New Customers", "Repeat Customers", "Avg Orders / Customer");
        assertThat(table.rows().get(0)).containsExactly(50L, 15L, 20L, new BigDecimal("2.35"));
    }

    @Test
    @DisplayName("payment analytics: one row per method with amount + currency, no re-aggregation")
    void paymentAnalytics() {
        Map<String, PaymentMethodStats> breakdown = new LinkedHashMap<>();
        breakdown.put("CARD", new PaymentMethodStats(45, new BigDecimal("48000")));
        breakdown.put("CASH", new PaymentMethodStats(20, new BigDecimal("15000")));
        when(analyticsService.getPaymentAnalytics(FROM, TO))
                .thenReturn(new PaymentAnalyticsResponse("VND", breakdown, FROM, TO));

        ReportTable table = service.buildPaymentAnalyticsReport(FROM, TO).tables().get(0);

        assertThat(table.columns()).extracting(c -> c.header())
                .containsExactly("Payment Method", "Orders", "Amount", "Currency");
        assertThat(table.rows()).hasSize(2);
        // deterministic ordering (sorted by method key)
        assertThat(table.rows().get(0)).containsExactly("CARD", 45L, new BigDecimal("48000"), "VND");
        assertThat(table.rows().get(1)).containsExactly("CASH", 20L, new BigDecimal("15000"), "VND");
    }

    @Test
    @DisplayName("payment analytics tolerates an empty/absent breakdown")
    void paymentAnalyticsEmpty() {
        when(analyticsService.getPaymentAnalytics(eq(null), eq(null)))
                .thenReturn(new PaymentAnalyticsResponse("VND", Map.of(), null, null));

        assertThat(service.buildPaymentAnalyticsReport(null, null).tables().get(0).rows()).isEmpty();
    }
}
