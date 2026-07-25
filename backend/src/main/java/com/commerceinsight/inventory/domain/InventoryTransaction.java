package com.commerceinsight.inventory.domain;

import com.commerceinsight.product.domain.Product;
import com.commerceinsight.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * InventoryTransaction — immutable audit record for every stock change.
 *
 * <p>Maps to the {@code inventory_transactions} table.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>IMMUTABLE: once created, this record MUST NOT be modified or deleted.</li>
 *   <li>Created automatically by InventoryTransactionService.record() — never manually.</li>
 *   <li>Every inventory quantity change MUST produce exactly one InventoryTransaction.</li>
 *   <li>For warehouse transfers, TWO transactions are created: TRANSFER_OUT + TRANSFER_IN.</li>
 * </ul>
 */
@Entity
@Table(name = "inventory_transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The inventory record that was changed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false, updatable = false)
    private Inventory inventory;

    /** The product whose stock changed (denormalized for query performance). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    /** The warehouse where the change occurred (denormalized for query performance). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, updatable = false)
    private Warehouse warehouse;

    /** The user who performed this change. Null if performed by system. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", updatable = false)
    private User performedBy;

    /**
     * Type of transaction.
     * Stored as VARCHAR(50) to match the CHECK constraint in the DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50, updatable = false)
    private TransactionType type;

    /**
     * Amount the stock changed.
     * Positive = stock added (purchase, return, transfer in).
     * Negative = stock removed (sale, damage, transfer out).
     */
    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    /** Quantity before this transaction was applied. */
    @Column(name = "quantity_before", nullable = false, updatable = false)
    private int quantityBefore;

    /** Quantity after this transaction was applied. */
    @Column(name = "quantity_after", nullable = false, updatable = false)
    private int quantityAfter;

    /**
     * Optional reference to a related entity (order_id, stock_adjustment_id, etc.).
     * Enables linking a transaction back to the business event that caused it.
     */
    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    /** Optional human-readable notes describing this transaction. */
    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
