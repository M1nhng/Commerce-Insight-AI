package com.commerceinsight.order.service;

import com.commerceinsight.order.domain.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * OrderCalculationService — calculates all monetary totals for an order.
 *
 * <p>Architecture rule: Backend ALWAYS recalculates totals.
 * Client-provided total values are NEVER trusted.
 *
 * <p>Formula:
 * <pre>
 *   itemSubtotal  = unitPrice * quantity - itemDiscountAmount
 *   subtotal      = sum(itemSubtotal for all items)
 *   total         = subtotal - orderDiscount + shippingFee + tax
 * </pre>
 */
@Service
public class OrderCalculationService {

    /**
     * Calculates the subtotal from a list of order items.
     * Each item's subtotal is (unitPrice * quantity) - discountAmount.
     *
     * @param items list of order items (must have unitPrice, quantity, discountAmount set)
     * @return the sum of all item subtotals
     */
    public BigDecimal calculateSubtotal(List<OrderItem> items) {
        return items.stream()
                .map(this::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates a single item's subtotal.
     */
    public BigDecimal calculateItemSubtotal(OrderItem item) {
        BigDecimal gross = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal discount = item.getDiscountAmount() != null
                ? item.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal result = gross.subtract(discount);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    /**
     * Calculates the grand total.
     *
     * @param subtotal     sum of all item subtotals
     * @param discount     order-level discount
     * @param shippingFee  shipping cost
     * @param tax          tax amount
     * @return grand total (never negative — floored at 0)
     */
    public BigDecimal calculateTotal(BigDecimal subtotal,
                                     BigDecimal discount,
                                     BigDecimal shippingFee,
                                     BigDecimal tax) {
        BigDecimal result = subtotal
                .subtract(nullSafe(discount))
                .add(nullSafe(shippingFee))
                .add(nullSafe(tax));
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
