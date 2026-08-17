package com.commerceinsight.order.service;

import com.commerceinsight.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderCalculationService.
 * No Spring context needed — pure logic tests.
 */
@DisplayName("OrderCalculationService")
class OrderCalculationServiceTest {

    private OrderCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new OrderCalculationService();
    }

    private OrderItem item(BigDecimal unitPrice, int qty, BigDecimal discount) {
        OrderItem item = new OrderItem();
        item.setUnitPrice(unitPrice);
        item.setQuantity(qty);
        item.setDiscountAmount(discount != null ? discount : BigDecimal.ZERO);
        item.setSubtotal(BigDecimal.ZERO);
        item.setTotal(BigDecimal.ZERO);
        item.setDiscount(BigDecimal.ZERO);
        return item;
    }

    // ── calculateItemSubtotal ────────────────────────────────────────────────

    @Test
    @DisplayName("itemSubtotal = unitPrice * qty - discount")
    void calculateItemSubtotal_basic() {
        OrderItem item = item(new BigDecimal("50.00"), 2, new BigDecimal("10.00"));
        BigDecimal result = calculationService.calculateItemSubtotal(item);
        assertThat(result).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("itemSubtotal floors at 0 when discount > gross")
    void calculateItemSubtotal_discountExceedsGross_floorsAtZero() {
        OrderItem item = item(new BigDecimal("10.00"), 1, new BigDecimal("999.00"));
        BigDecimal result = calculationService.calculateItemSubtotal(item);
        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("itemSubtotal with null discount treated as 0")
    void calculateItemSubtotal_nullDiscount() {
        OrderItem item = item(new BigDecimal("25.00"), 4, null);
        BigDecimal result = calculationService.calculateItemSubtotal(item);
        assertThat(result).isEqualByComparingTo("100.00");
    }

    // ── calculateSubtotal ────────────────────────────────────────────────────

    @Test
    @DisplayName("subtotal = sum of all item subtotals")
    void calculateSubtotal_multipleItems() {
        List<OrderItem> items = List.of(
                item(new BigDecimal("100.00"), 2, BigDecimal.ZERO),  // 200
                item(new BigDecimal("50.00"),  3, new BigDecimal("10.00")) // 140
        );
        BigDecimal result = calculationService.calculateSubtotal(items);
        assertThat(result).isEqualByComparingTo("340.00");
    }

    @Test
    @DisplayName("subtotal of empty list is 0")
    void calculateSubtotal_emptyList() {
        BigDecimal result = calculationService.calculateSubtotal(List.of());
        assertThat(result).isEqualByComparingTo("0.00");
    }

    // ── calculateTotal ───────────────────────────────────────────────────────

    @Test
    @DisplayName("total = subtotal - discount + shippingFee + tax")
    void calculateTotal_allFields() {
        BigDecimal total = calculationService.calculateTotal(
                new BigDecimal("500.00"),  // subtotal
                new BigDecimal("50.00"),   // discount
                new BigDecimal("20.00"),   // shipping
                new BigDecimal("30.00")    // tax
        );
        assertThat(total).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("total floors at 0 when discount is massive")
    void calculateTotal_hugeDiscount_floorsAtZero() {
        BigDecimal total = calculationService.calculateTotal(
                new BigDecimal("100.00"),
                new BigDecimal("9999.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        assertThat(total).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("total with null fields treated as 0")
    void calculateTotal_nullFields() {
        BigDecimal total = calculationService.calculateTotal(
                new BigDecimal("300.00"), null, null, null);
        assertThat(total).isEqualByComparingTo("300.00");
    }
}
