package com.commerceinsight.inventory.domain;

/**
 * AdjustmentStatus — lifecycle states for a {@link StockAdjustment}.
 *
 * <ul>
 *   <li>PENDING — submitted, awaiting manager/admin review</li>
 *   <li>APPROVED — reviewed and applied to inventory</li>
 *   <li>REJECTED — reviewed and declined; no stock change made</li>
 * </ul>
 *
 * <p>State machine: PENDING → APPROVED | REJECTED (terminal states)
 */
public enum AdjustmentStatus {
    PENDING,
    APPROVED,
    REJECTED
}
