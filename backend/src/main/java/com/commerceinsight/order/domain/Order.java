package com.commerceinsight.order.domain;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order — core order entity representing a customer purchase transaction.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>Orders are NEVER soft-deleted. Status-only lifecycle.</li>
 *   <li>Totals (subtotal, tax, shipping, total) are always calculated by
 *       {@code OrderCalculationService} — never accepted from the client.</li>
 *   <li>Status transitions are enforced by {@code OrderStatusTransitionService}.</li>
 *   <li>Never expose this entity beyond the service layer. Use DTOs.</li>
 * </ul>
 *
 * <p>Maps to the {@code orders} table (created in V6, extended in V23).
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    /**
     * Human-readable order identifier. Format: ORD-{yyyyMM}-{6digits}.
     * Unique — enforced by DB unique constraint.
     */
    @Column(name = "order_number", nullable = false, length = 50, unique = true)
    private String orderNumber;

    /**
     * The customer who placed this order.
     * SET NULL on customer delete to preserve order history.
     * LAZY loaded — use customer snapshot fields in items for display.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Current order status. Transitions controlled by OrderStatusTransitionService.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /** Sum of (unitPrice * quantity - itemDiscount) across all items. */
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Order-level discount applied to subtotal. */
    @Column(name = "discount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    /** Shipping fee. */
    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /** Tax amount. */
    @Column(name = "tax", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    /** Grand total: subtotal - discount + shippingFee + tax. */
    @Column(name = "total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /** ISO 4217 currency code. Default: VND. */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /** Payment lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /** Free-text notes from the person who created the order. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Set when order transitions to SHIPPED. */
    @Column(name = "shipped_at")
    private Instant shippedAt;

    /** Set when order transitions to DELIVERED. */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /** Set when order transitions to CANCELLED. */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /** Set when order transitions to COMPLETED. */
    @Column(name = "completed_at")
    private Instant completedAt;

    // ── Relationships ─────────────────────────────────────────────────────────

    /** Line items of this order. Cascade — items live and die with the order. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /** Address snapshots (SHIPPING + BILLING). Immutable once created. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderAddress> addresses = new ArrayList<>();

    /** Status change audit trail. Append-only. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    /** Payment record. One per order. */
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    // ── Domain helpers ────────────────────────────────────────────────────────

    /** Recalculates and sets the grand total. Called by OrderCalculationService. */
    public void recalculateTotal() {
        this.total = subtotal.subtract(discount).add(shippingFee).add(tax);
    }
}
