package com.commerceinsight.inventory.domain;

import com.commerceinsight.product.domain.Product;
import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Inventory — current stock level for a product at a specific warehouse.
 *
 * <p>Maps to the {@code inventory} table (extended in V15 with warehouse support).
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never expose this entity beyond the service layer. Use DTOs.</li>
 *   <li>No soft delete — inventory records are permanent; use InventoryTransaction for history.</li>
 *   <li>quantity MUST always be >= 0. Enforced at DB level (CHECK) and service level.</li>
 *   <li>Every change to quantity MUST create an InventoryTransaction record.</li>
 *   <li>Unique constraint: (product_id, warehouse_id) — one record per product per warehouse.</li>
 * </ul>
 */
@Entity
@Table(name = "inventory",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_inventory_product_warehouse",
               columnNames = {"product_id", "warehouse_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    /**
     * The product whose stock this record tracks.
     * LAZY-loaded for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The warehouse where this stock is held.
     * LAZY-loaded for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /**
     * Total units of this product currently on hand at this warehouse.
     * INVARIANT: quantity >= 0. Never go negative.
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private int quantity = 0;

    /**
     * Units reserved for confirmed orders that have not yet shipped.
     * Available stock = quantity - reservedQuantity.
     * INVARIANT: reservedQuantity >= 0 and reservedQuantity <= quantity.
     */
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    /**
     * Threshold below which this product is considered "low stock".
     * An alert is triggered when: quantity <= lowStockThreshold.
     */
    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private int lowStockThreshold = 10;

    /**
     * Returns how much stock is currently available for new orders.
     * Available = on-hand minus already-reserved.
     */
    public int getAvailableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }

    /**
     * Returns true if the current quantity is at or below the low-stock threshold.
     */
    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }
}
