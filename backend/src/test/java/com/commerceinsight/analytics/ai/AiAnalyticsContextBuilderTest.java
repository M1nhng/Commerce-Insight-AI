package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.dto.CustomerAnalyticsResponse;
import com.commerceinsight.analytics.dto.OrderAnalyticsResponse;
import com.commerceinsight.analytics.dto.OverviewResponse;
import com.commerceinsight.analytics.dto.PaymentAnalyticsResponse;
import com.commerceinsight.analytics.dto.PaymentMethodStats;
import com.commerceinsight.analytics.dto.RevenuePeriodResponse;
import com.commerceinsight.analytics.dto.RevenueResponse;
import com.commerceinsight.analytics.dto.TopProductEntry;
import com.commerceinsight.analytics.service.AnalyticsService;
import com.commerceinsight.inventory.dto.response.InventoryResponse;
import com.commerceinsight.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiAnalyticsContextBuilder} — it only COMPOSES the
 * existing {@link AnalyticsService}; it must map fields faithfully, compute a
 * previous-period baseline, and expose aggregates only (no PII).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiAnalyticsContextBuilder")
class AiAnalyticsContextBuilderTest {

    @Mock private AnalyticsService analyticsService;
    @Mock private InventoryService inventoryService;
    @InjectMocks private AiAnalyticsContextBuilder builder;

    private final Instant from = Instant.parse("2026-02-01T00:00:00Z");
    private final Instant to   = Instant.parse("2026-03-01T00:00:00Z");

    private void stubAnalytics(BigDecimal currentRevenue, BigDecimal previousRevenue) {
        // current window overview
        when(analyticsService.getOverview(eq(from), eq(to))).thenReturn(new OverviewResponse(
                currentRevenue, 100, 40, 250, new BigDecimal("35000"), 5, new BigDecimal("5.00"),
                "VND", from, to));
        // previous equal-length window overview (28 days earlier)
        when(analyticsService.getOverview(eq(from.minusSeconds(28L * 86400)), eq(from)))
                .thenReturn(new OverviewResponse(previousRevenue, 80, 30, 200, new BigDecimal("30000"),
                        4, new BigDecimal("5.00"), "VND", null, null));

        when(analyticsService.getOrderAnalytics(eq(from), eq(to))).thenReturn(new OrderAnalyticsResponse(
                100, 10, 20, 15, 8, 7, 35, 5, new BigDecimal("35.00"), new BigDecimal("5.00"), from, to));

        when(analyticsService.getRevenue(eq(from), eq(to), eq("MONTH"))).thenReturn(new RevenueResponse(
                "MONTH", "VND", from, to, List.of(
                new RevenuePeriodResponse("2026-02", currentRevenue, 100))));

        when(analyticsService.getTopProducts(eq(from), eq(to), anyInt())).thenReturn(List.of(
                new TopProductEntry(UUID.randomUUID(), "SKU-1", "Alpha Widget", 40, new BigDecimal("1000000"))));

        when(analyticsService.getCustomerAnalytics(eq(from), eq(to))).thenReturn(new CustomerAnalyticsResponse(
                40, 12, 9, new BigDecimal("2.50"), from, to));

        when(analyticsService.getPaymentAnalytics(eq(from), eq(to))).thenReturn(new PaymentAnalyticsResponse(
                "VND", Map.of("CARD", new PaymentMethodStats(60, new BigDecimal("3000000"))), from, to));
    }

    @Test
    @DisplayName("maps every analytics DTO into the compact context")
    void mapsFields() {
        stubAnalytics(new BigDecimal("2000000"), new BigDecimal("1000000"));
        when(inventoryService.findLowStock()).thenReturn(List.of(
                lowStock(3), lowStock(0), lowStock(-1)));

        AiAnalyticsContext ctx = builder.build(from, to);

        assertThat(ctx.currency()).isEqualTo("VND");
        assertThat(ctx.overview().totalRevenue()).isEqualByComparingTo("2000000");
        assertThat(ctx.overview().totalOrders()).isEqualTo(100);
        // revenue-eligible = confirmed+processing+shipped+delivered+completed = 20+15+8+7+35
        assertThat(ctx.overview().revenueEligibleOrders()).isEqualTo(85);
        assertThat(ctx.ordersByStatus()).containsEntry("CANCELLED", 5L).containsEntry("COMPLETED", 35L);
        assertThat(ctx.topProducts()).singleElement()
                .satisfies(tp -> assertThat(tp.productName()).isEqualTo("Alpha Widget"));
        assertThat(ctx.paymentMethods()).containsKey("CARD");
        assertThat(ctx.customers().uniqueCustomers()).isEqualTo(40);
        assertThat(ctx.revenueByMonth()).hasSize(1);
        assertThat(ctx.inventory().lowStockItems()).isEqualTo(3);
        assertThat(ctx.inventory().outOfStockItems()).isEqualTo(2); // availableQuantity <= 0
    }

    @Test
    @DisplayName("computes revenue growth vs the preceding equal-length window")
    void computesGrowth() {
        stubAnalytics(new BigDecimal("2000000"), new BigDecimal("1000000"));
        when(inventoryService.findLowStock()).thenReturn(List.of());

        AiAnalyticsContext ctx = builder.build(from, to);

        assertThat(ctx.growth().previousPeriodRevenue()).isEqualByComparingTo("1000000");
        assertThat(ctx.growth().revenueGrowthPct()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("growth percentage is null (not fabricated) when the baseline is zero")
    void zeroBaseline_noGrowthPct() {
        stubAnalytics(new BigDecimal("2000000"), BigDecimal.ZERO);
        when(inventoryService.findLowStock()).thenReturn(List.of());

        AiAnalyticsContext ctx = builder.build(from, to);

        assertThat(ctx.growth().revenueGrowthPct()).isNull();
        assertThat(ctx.growth().note()).containsIgnoringCase("not defined");
    }

    @Test
    @DisplayName("serialised context carries no PII field names")
    void serialisedContext_noPii() throws Exception {
        stubAnalytics(new BigDecimal("2000000"), new BigDecimal("1000000"));
        when(inventoryService.findLowStock()).thenReturn(List.of());

        String json = new ObjectMapper().writeValueAsString(builder.build(from, to)).toLowerCase();

        assertThat(json).doesNotContain("email");
        assertThat(json).doesNotContain("phone");
        assertThat(json).doesNotContain("password");
        assertThat(json).doesNotContain("customerid");
        assertThat(json).doesNotContain("token");
    }

    @Test
    @DisplayName("inventory failure degrades to a zeroed summary, not an error")
    void inventoryFailure_isSoft() {
        stubAnalytics(new BigDecimal("2000000"), new BigDecimal("1000000"));
        when(inventoryService.findLowStock()).thenThrow(new RuntimeException("db down"));

        AiAnalyticsContext ctx = builder.build(from, to);

        assertThat(ctx.inventory().lowStockItems()).isZero();
        assertThat(ctx.inventory().outOfStockItems()).isZero();
    }

    private static InventoryResponse lowStock(int available) {
        return new InventoryResponse(UUID.randomUUID(), UUID.randomUUID(), "P", "S",
                UUID.randomUUID(), "W", "WC", Math.max(available, 0), 0, available, 10, true, Instant.now());
    }
}
