package com.commerceinsight.inventory.domain;

/**
 * TransactionType — type of inventory movement.
 *
 * <p>Used by {@link InventoryTransaction} to categorize why stock changed.
 *
 * <ul>
 *   <li>PURCHASE — stock received from a supplier</li>
 *   <li>SALE — stock deducted when an order ships</li>
 *   <li>ADJUSTMENT — manual correction (requires approval via StockAdjustment)</li>
 *   <li>RETURN — customer return adding stock back</li>
 *   <li>DAMAGE — stock written off due to damage or expiry</li>
 *   <li>TRANSFER_IN — stock arriving from another warehouse</li>
 *   <li>TRANSFER_OUT — stock leaving to another warehouse</li>
 * </ul>
 */
public enum TransactionType {
    PURCHASE,
    SALE,
    ADJUSTMENT,
    RETURN,
    DAMAGE,
    TRANSFER_IN,
    TRANSFER_OUT
}
