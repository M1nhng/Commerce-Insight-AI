package com.commerceinsight.order.domain;

import com.commerceinsight.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * OrderStatusHistory — append-only audit trail of every status transition.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>Records are NEVER updated or deleted (beyond CASCADE with parent order).</li>
 *   <li>Every valid status transition in {@code OrderStatusTransitionService}
 *       must create exactly one record here.</li>
 *   <li>No BaseEntity — purely append-only; no updatedAt needed.</li>
 * </ul>
 *
 * <p>Maps to the {@code order_status_history} table (created in V25).
 */
@Entity
@Table(name = "order_status_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Parent order. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Status before the transition. Null for the initial PENDING entry
     * (order created — no previous status).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 50)
    private OrderStatus fromStatus;

    /** Status after the transition. */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 50)
    private OrderStatus toStatus;

    /**
     * The user who triggered this transition.
     * Null = system-generated (e.g., automatic completion).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    /** Optional human-readable reason for the transition. */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
