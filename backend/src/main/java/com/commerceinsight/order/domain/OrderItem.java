package com.commerceinsight.order.domain;

import com.commerceinsight.product.domain.Product;
import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * OrderItem — a single line item within an order.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>Product name and SKU are deliberately denormalized (snapshots) to preserve
 *       historical order data even if the product is later modified or deleted.</li>
 *   <li>unit_price is the price at the time of order creation — NOT the current product price.</li>
 *   <li>subtotal = (unitPrice * quantity) - discountAmount</li>
 *   <li>No soft delete — items cascade-delete with the parent order.</li>
 * </ul>
 *
 * <p>Maps to the {@code order_items} table (created in V6, extended in V23).
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    /** Parent order. Required. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Reference to the product — may become null if the product is deleted.
     * ALWAYS use snapshot fields for display; never use product.name or product.price.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * Product SKU captured at the time this order was placed.
     * Intentionally denormalized — immutable historical snapshot.
     */
    @Column(name = "sku_snapshot", nullable = false, length = 100)
    private String skuSnapshot;

    /**
     * Product name captured at the time this order was placed.
     * Intentionally denormalized — immutable historical snapshot.
     */
    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    /**
     * Also stored in legacy column product_sku (V6) for backward compatibility.
     * Kept in sync with skuSnapshot.
     */
    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;

    /**
     * Also stored in legacy column product_name (V6) for backward compatibility.
     * Kept in sync with productNameSnapshot.
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * Selling price per unit at the time of order. NOT the current product price.
     */
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /** Number of units ordered. Must be > 0. */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** Per-line-item discount amount. Default 0. */
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Line-item subtotal: (unitPrice * quantity) - discountAmount.
     * Also stored in legacy column 'total' (V6) for backward compatibility.
     */
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    /**
     * Legacy total column from V6. Kept in sync with subtotal.
     */
    @Column(name = "total", nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    /**
     * Legacy discount column from V6. Kept in sync with discountAmount.
     */
    @Column(name = "discount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    // ── Domain helper ─────────────────────────────────────────────────────────

    /**
     * Calculates and sets subtotal = (unitPrice * quantity) - discountAmount.
     * Also syncs legacy columns.
     */
    public void calculateSubtotal() {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.subtotal = gross.subtract(discountAmount);
        this.total = this.subtotal;             // legacy column sync
        this.discount = this.discountAmount;    // legacy column sync
        this.productSku = this.skuSnapshot;
        this.productName = this.productNameSnapshot;
    }
}
