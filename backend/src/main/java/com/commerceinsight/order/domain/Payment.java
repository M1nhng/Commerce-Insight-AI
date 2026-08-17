package com.commerceinsight.order.domain;

import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment — simulated payment record for an order.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>One payment record per order (1:1 relationship).</li>
 *   <li>No external payment gateway integration — status managed internally.</li>
 *   <li>amount always mirrors the order's total_amount at creation time.</li>
 * </ul>
 *
 * <p>Maps to the {@code payments} table (created in V26).
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    /** The order this payment is for. 1:1 relationship. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    /** Payment method chosen by the operator. */
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 50)
    private PaymentMethod method;

    /** Payment lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Total payment amount — mirrors order.total at creation time.
     * Stored separately in case of partial refunds in future sprints.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** ISO 4217 currency code. Default: VND. */
    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    /**
     * Optional external payment reference (e.g., bank transaction code).
     * Used for manual BANK_TRANSFER verification.
     */
    @Column(name = "reference", length = 255)
    private String reference;

    /** Timestamp when this payment transitioned to PAID. */
    @Column(name = "paid_at")
    private Instant paidAt;

    /** Free-text notes from the operator. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
