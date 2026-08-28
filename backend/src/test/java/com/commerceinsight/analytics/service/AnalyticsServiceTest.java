package com.commerceinsight.analytics.service;

import com.commerceinsight.analytics.dto.*;
import com.commerceinsight.analytics.repository.AnalyticsRepository;
import com.commerceinsight.analytics.repository.projection.CustomerOrderCountRow;
import com.commerceinsight.analytics.repository.projection.PaymentMethodRow;
import com.commerceinsight.analytics.repository.projection.RevenueTimeSeriesRow;
import com.commerceinsight.analytics.repository.projection.StatusCountRow;
import com.commerceinsight.analytics.repository.projection.TopProductRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService.
 *
 * <p>No Spring context — pure unit tests with Mockito.
 * All AnalyticsRepository methods are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService")
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private static final Instant FROM = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant TO   = Instant.parse("2026-08-31T23:59:59Z");

    // ── Helper factories ──────────────────────────────────────────────────────

    private StatusCountRow statusRow(String status, long count) {
        return new StatusCountRow() {
            public String getStatus()     { return status; }
            public long   getOrderCount() { return count;  }
        };
    }

    private RevenueTimeSeriesRow tsRow(String period, BigDecimal revenue, long orders) {
        return new RevenueTimeSeriesRow() {
            public String     getPeriod()  { return period;  }
            public BigDecimal getRevenue() { return revenue; }
            public long       getOrders()  { return orders;  }
        };
    }

    private TopProductRow topRow(String sku, String name, long qty, BigDecimal rev) {
        UUID id = UUID.randomUUID();
        return new TopProductRow() {
            public UUID      getProductId()   { return id;   }
            public String    getSku()         { return sku;  }
            public String    getProductName() { return name; }
            public long      getQuantitySold(){ return qty;  }
            public BigDecimal getRevenue()    { return rev;  }
        };
    }

    private CustomerOrderCountRow custRow(long inPeriod, long allTime) {
        return new CustomerOrderCountRow() {
            public UUID getCustomerId()         { return UUID.randomUUID(); }
            public long getOrdersInPeriod()     { return inPeriod; }
            public long getTotalOrdersAllTime()  { return allTime;  }
        };
    }

    private PaymentMethodRow pmtRow(String method, long orders, BigDecimal amount) {
        return new PaymentMethodRow() {
            public String     getMethod() { return method; }
            public long       getOrders() { return orders; }
            public BigDecimal getAmount() { return amount; }
        };
    }

    // ── Overview tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOverview")
    class GetOverview {

        @BeforeEach
        void setupCommonMocks() {
            // status breakdown is used inside countRevenueEligibleOrders
            when(analyticsRepository.countByStatus(FROM, TO)).thenReturn(List.of(
                    statusRow("PENDING",    5),
                    statusRow("CONFIRMED",  10),
                    statusRow("PROCESSING", 8),
                    statusRow("COMPLETED",  12),
                    statusRow("CANCELLED",  5)
            ));
        }

        @Test
        @DisplayName("returns correct KPIs for normal data")
        void overview_normalData() {
            when(analyticsRepository.sumRevenue(FROM, TO))
                    .thenReturn(new BigDecimal("120000.00"));
            when(analyticsRepository.countAllOrders(FROM, TO)).thenReturn(40L);
            when(analyticsRepository.countDistinctCustomers(FROM, TO)).thenReturn(25L);
            when(analyticsRepository.sumProductsSold(FROM, TO)).thenReturn(150L);
            when(analyticsRepository.countCancelledOrders(FROM, TO)).thenReturn(5L);

            OverviewResponse result = analyticsService.getOverview(FROM, TO);

            // totalOrders = 40
            assertThat(result.totalOrders()).isEqualTo(40L);
            // totalRevenue = 120000
            assertThat(result.totalRevenue()).isEqualByComparingTo("120000.00");
            // revenue-eligible orders = 10+8+12 = 30 → AOV = 120000/30 = 4000
            assertThat(result.averageOrderValue()).isEqualByComparingTo("4000.00");
            // cancellationRate = 5/40 * 100 = 12.50
            assertThat(result.cancellationRate()).isEqualByComparingTo("12.50");
            assertThat(result.totalCustomers()).isEqualTo(25L);
            assertThat(result.totalProductsSold()).isEqualTo(150L);
            assertThat(result.cancelledOrders()).isEqualTo(5L);
            assertThat(result.currency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("averageOrderValue is 0 when there are no revenue-eligible orders")
        void overview_zeroRevenueEligibleOrders() {
            // Only CANCELLED orders → no revenue-eligible statuses
            when(analyticsRepository.countByStatus(FROM, TO)).thenReturn(List.of(
                    statusRow("CANCELLED", 3)
            ));
            when(analyticsRepository.sumRevenue(FROM, TO)).thenReturn(BigDecimal.ZERO);
            when(analyticsRepository.countAllOrders(FROM, TO)).thenReturn(3L);
            when(analyticsRepository.countDistinctCustomers(FROM, TO)).thenReturn(2L);
            when(analyticsRepository.sumProductsSold(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.countCancelledOrders(FROM, TO)).thenReturn(3L);

            OverviewResponse result = analyticsService.getOverview(FROM, TO);

            assertThat(result.averageOrderValue()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("cancellationRate is 0 when no orders exist")
        void overview_noOrders() {
            when(analyticsRepository.countByStatus(FROM, TO)).thenReturn(List.of());
            when(analyticsRepository.sumRevenue(FROM, TO)).thenReturn(null);
            when(analyticsRepository.countAllOrders(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.countDistinctCustomers(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.sumProductsSold(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.countCancelledOrders(FROM, TO)).thenReturn(0L);

            OverviewResponse result = analyticsService.getOverview(FROM, TO);

            assertThat(result.totalOrders()).isEqualTo(0L);
            assertThat(result.totalRevenue()).isEqualByComparingTo("0.00");
            assertThat(result.averageOrderValue()).isEqualByComparingTo("0.00");
            assertThat(result.cancellationRate()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("null revenue from repository is treated as zero")
        void overview_nullRevenueFromRepository() {
            when(analyticsRepository.sumRevenue(FROM, TO)).thenReturn(null);
            when(analyticsRepository.countAllOrders(FROM, TO)).thenReturn(5L);
            when(analyticsRepository.countDistinctCustomers(FROM, TO)).thenReturn(3L);
            when(analyticsRepository.sumProductsSold(FROM, TO)).thenReturn(0L);
            when(analyticsRepository.countCancelledOrders(FROM, TO)).thenReturn(0L);

            OverviewResponse result = analyticsService.getOverview(FROM, TO);

            assertThat(result.totalRevenue()).isEqualByComparingTo("0.00");
        }
    }

    // ── Revenue time series tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("getRevenue")
    class GetRevenue {

        @Test
        @DisplayName("returns DAY series by default")
        void revenue_defaultGroupByDay() {
            when(analyticsRepository.revenueByDay(FROM, TO)).thenReturn(List.of(
                    tsRow("2026-08-01", new BigDecimal("5000"), 5),
                    tsRow("2026-08-02", new BigDecimal("3000"), 3)
            ));

            RevenueResponse result = analyticsService.getRevenue(FROM, TO, null);

            assertThat(result.groupBy()).isEqualTo("DAY");
            assertThat(result.data()).hasSize(2);
            assertThat(result.data().get(0).period()).isEqualTo("2026-08-01");
            assertThat(result.data().get(0).revenue()).isEqualByComparingTo("5000");
            assertThat(result.data().get(0).orders()).isEqualTo(5L);
        }

        @Test
        @DisplayName("delegates to revenueByMonth when groupBy=MONTH")
        void revenue_monthGrouping() {
            when(analyticsRepository.revenueByMonth(FROM, TO)).thenReturn(List.of(
                    tsRow("2026-08", new BigDecimal("48000"), 40)
            ));

            RevenueResponse result = analyticsService.getRevenue(FROM, TO, "MONTH");

            assertThat(result.groupBy()).isEqualTo("MONTH");
            assertThat(result.data()).hasSize(1);
            verify(analyticsRepository).revenueByMonth(FROM, TO);
            verify(analyticsRepository, never()).revenueByDay(any(), any());
        }

        @Test
        @DisplayName("delegates to revenueByWeek when groupBy=WEEK")
        void revenue_weekGrouping() {
            when(analyticsRepository.revenueByWeek(FROM, TO)).thenReturn(List.of());
            analyticsService.getRevenue(FROM, TO, "WEEK");
            verify(analyticsRepository).revenueByWeek(FROM, TO);
        }

        @Test
        @DisplayName("returns empty data list when no eligible orders")
        void revenue_empty() {
            when(analyticsRepository.revenueByDay(null, null)).thenReturn(List.of());
            RevenueResponse result = analyticsService.getRevenue(null, null, "DAY");
            assertThat(result.data()).isEmpty();
            assertThat(result.currency()).isEqualTo("VND");
        }
    }

    // ── Order analytics tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrderAnalytics")
    class GetOrderAnalytics {

        @Test
        @DisplayName("calculates completion and cancellation rates correctly")
        void orderAnalytics_rates() {
            when(analyticsRepository.countByStatus(FROM, TO)).thenReturn(List.of(
                    statusRow("COMPLETED", 60),
                    statusRow("CANCELLED", 20),
                    statusRow("PENDING",   20)
            ));

            OrderAnalyticsResponse result = analyticsService.getOrderAnalytics(FROM, TO);

            assertThat(result.totalOrders()).isEqualTo(100L);
            assertThat(result.completedOrders()).isEqualTo(60L);
            assertThat(result.cancelledOrders()).isEqualTo(20L);
            // completionRate = 60/100 * 100 = 60.00
            assertThat(result.completionRate()).isEqualByComparingTo("60.00");
            // cancellationRate = 20/100 * 100 = 20.00
            assertThat(result.cancellationRate()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("rates are 0 when no orders exist")
        void orderAnalytics_noOrders() {
            when(analyticsRepository.countByStatus(FROM, TO)).thenReturn(List.of());

            OrderAnalyticsResponse result = analyticsService.getOrderAnalytics(FROM, TO);

            assertThat(result.totalOrders()).isEqualTo(0L);
            assertThat(result.completionRate()).isEqualByComparingTo("0.00");
            assertThat(result.cancellationRate()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("missing status buckets default to 0")
        void orderAnalytics_missingStatuses() {
            // Only COMPLETED present — all others should be 0
            when(analyticsRepository.countByStatus(FROM, TO)).thenReturn(List.of(
                    statusRow("COMPLETED", 10)
            ));

            OrderAnalyticsResponse result = analyticsService.getOrderAnalytics(FROM, TO);

            assertThat(result.pendingOrders()).isEqualTo(0L);
            assertThat(result.cancelledOrders()).isEqualTo(0L);
            assertThat(result.completionRate()).isEqualByComparingTo("100.00");
        }
    }

    // ── Top products tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTopProducts")
    class GetTopProducts {

        @Test
        @DisplayName("returns top products mapped correctly")
        void topProducts_normal() {
            when(analyticsRepository.topProductsByRevenue(FROM, TO, 10)).thenReturn(List.of(
                    topRow("SKU-001", "Product A", 50, new BigDecimal("25000")),
                    topRow("SKU-002", "Product B", 30, new BigDecimal("12000"))
            ));

            List<TopProductEntry> result = analyticsService.getTopProducts(FROM, TO, 10);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).sku()).isEqualTo("SKU-001");
            assertThat(result.get(0).quantitySold()).isEqualTo(50L);
            assertThat(result.get(0).revenue()).isEqualByComparingTo("25000");
        }

        @Test
        @DisplayName("clamps limit to 50 maximum")
        void topProducts_limitClamped() {
            when(analyticsRepository.topProductsByRevenue(FROM, TO, 50)).thenReturn(List.of());
            analyticsService.getTopProducts(FROM, TO, 999);
            verify(analyticsRepository).topProductsByRevenue(FROM, TO, 50);
        }

        @Test
        @DisplayName("clamps limit to 1 minimum")
        void topProducts_limitMinimum() {
            when(analyticsRepository.topProductsByRevenue(FROM, TO, 1)).thenReturn(List.of());
            analyticsService.getTopProducts(FROM, TO, 0);
            verify(analyticsRepository).topProductsByRevenue(FROM, TO, 1);
        }

        @Test
        @DisplayName("returns empty list when no products sold")
        void topProducts_empty() {
            when(analyticsRepository.topProductsByRevenue(FROM, TO, 10)).thenReturn(List.of());
            List<TopProductEntry> result = analyticsService.getTopProducts(FROM, TO, 10);
            assertThat(result).isEmpty();
        }
    }

    // ── Customer analytics tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerAnalytics")
    class GetCustomerAnalytics {

        @Test
        @DisplayName("correctly distinguishes new vs repeat customers")
        void customerAnalytics_newAndRepeat() {
            // Customer A: 1 order in period, 1 total → NEW
            // Customer B: 3 orders in period, 5 total → REPEAT (in period) + returning
            // Customer C: 1 order in period, 3 total → NOT new (had previous orders)
            when(analyticsRepository.customerOrderCounts(FROM, TO)).thenReturn(List.of(
                    custRow(1, 1), // new, not repeat
                    custRow(3, 5), // not new, repeat (>1 in period)
                    custRow(1, 3)  // not new, not repeat
            ));

            CustomerAnalyticsResponse result = analyticsService.getCustomerAnalytics(FROM, TO);

            assertThat(result.uniqueCustomers()).isEqualTo(3L);
            assertThat(result.newCustomers()).isEqualTo(1L);     // only A
            assertThat(result.repeatCustomers()).isEqualTo(1L);  // only B
            // totalOrders = 1+3+1=5, unique=3 → avg = 5/3 = 1.67
            assertThat(result.averageOrdersPerCustomer()).isEqualByComparingTo("1.67");
        }

        @Test
        @DisplayName("averageOrdersPerCustomer is 0 when no customers")
        void customerAnalytics_noCustomers() {
            when(analyticsRepository.customerOrderCounts(FROM, TO)).thenReturn(List.of());

            CustomerAnalyticsResponse result = analyticsService.getCustomerAnalytics(FROM, TO);

            assertThat(result.uniqueCustomers()).isEqualTo(0L);
            assertThat(result.averageOrdersPerCustomer()).isEqualByComparingTo("0.00");
        }
    }

    // ── Payment analytics tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("getPaymentAnalytics")
    class GetPaymentAnalytics {

        @Test
        @DisplayName("maps payment methods to stats correctly")
        void paymentAnalytics_normal() {
            when(analyticsRepository.paymentBreakdown(FROM, TO)).thenReturn(List.of(
                    pmtRow("CARD",          45, new BigDecimal("48000")),
                    pmtRow("CASH",          20, new BigDecimal("15000")),
                    pmtRow("BANK_TRANSFER",  5, new BigDecimal("7500"))
            ));

            PaymentAnalyticsResponse result = analyticsService.getPaymentAnalytics(FROM, TO);

            assertThat(result.breakdown()).containsKey("CARD");
            assertThat(result.breakdown().get("CARD").orders()).isEqualTo(45L);
            assertThat(result.breakdown().get("CARD").amount()).isEqualByComparingTo("48000");
            assertThat(result.breakdown()).containsKey("CASH");
            assertThat(result.breakdown()).containsKey("BANK_TRANSFER");
            assertThat(result.breakdown()).doesNotContainKey("OTHER");
            assertThat(result.currency()).isEqualTo("VND");
        }

        @Test
        @DisplayName("returns empty breakdown when no payments in period")
        void paymentAnalytics_empty() {
            when(analyticsRepository.paymentBreakdown(FROM, TO)).thenReturn(List.of());

            PaymentAnalyticsResponse result = analyticsService.getPaymentAnalytics(FROM, TO);

            assertThat(result.breakdown()).isEmpty();
        }

        @Test
        @DisplayName("null amount from repository is treated as zero")
        void paymentAnalytics_nullAmount() {
            when(analyticsRepository.paymentBreakdown(FROM, TO)).thenReturn(List.of(
                    pmtRow("CASH", 2, null)
            ));

            PaymentAnalyticsResponse result = analyticsService.getPaymentAnalytics(FROM, TO);

            assertThat(result.breakdown().get("CASH").amount()).isEqualByComparingTo("0.00");
        }
    }
}
