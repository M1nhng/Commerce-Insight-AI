package com.commerceinsight.inventory.domain;

import com.commerceinsight.product.domain.Product;
import com.commerceinsight.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * StockAdjustment — a requested inventory correction requiring approval.
 *
 * <p>Maps to the {@code stock_adjustments} table.
 *
 * <p>Lifecycle: PENDING → APPROVED (stock applied) | REJECTED (no stock change)
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Only PENDING adjustments can be approved or rejected.</li>
 *   <li>When APPROVED, the service creates an InventoryTransaction of type ADJUSTMENT.</li>
 *   <li>The resulting transaction is linked back via {@code transactionId}.</li>
 *   <li>STAFF can request adjustments; only ADMIN can approve/reject.</li>
 * </ul>
 */
@Entity
@Table(name = "stock_adjustments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The inventory record this adjustment targets. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    /** The product (denormalized for query convenience). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** The warehouse (denormalized for query convenience). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * How much stock to add (positive) or remove (negative) from inventory.
     * Example: +50 means "add 50 units", -10 means "remove 10 units".
     * Applied only when the adjustment is APPROVED.
     */
    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    /** The reason for requesting this adjustment. Required. */
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AdjustmentStatus status = AdjustmentStatus.PENDING;

    /** The user who requested this adjustment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    /** The admin who reviewed (approved or rejected) this adjustment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    /** When the review decision was made. */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /** Optional reviewer notes explaining the decision. */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /**
     * ID of the InventoryTransaction created when this adjustment was APPROVED.
     * Null while PENDING or REJECTED.
     */
    @Column(name = "transaction_id")
    private UUID transactionId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Returns true if this adjustment is still awaiting review. */
    public boolean isPending() {
        return status == AdjustmentStatus.PENDING;
    }
}
