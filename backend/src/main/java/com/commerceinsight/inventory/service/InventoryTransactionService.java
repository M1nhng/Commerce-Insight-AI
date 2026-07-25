package com.commerceinsight.inventory.service;

import com.commerceinsight.inventory.domain.*;
import com.commerceinsight.inventory.repository.InventoryTransactionRepository;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * InventoryTransactionService — responsible for recording every stock change.
 *
 * <p>Architecture Rule:
 * <ul>
 *   <li>This service MUST be called for every inventory quantity modification.</li>
 *   <li>Transactions are IMMUTABLE once created. No updates or deletes.</li>
 *   <li>Runs within the calling transaction (REQUIRED propagation) to ensure
 *       the inventory update and its transaction record are always committed together.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryTransactionRepository transactionRepository;

    /**
     * Records an inventory change as an immutable InventoryTransaction.
     *
     * <p>Called after updating the Inventory entity's quantity.
     * {@code quantityBefore} and {@code quantityAfter} are snapshots captured
     * by the caller before/after modification.
     *
     * @param inventory      the inventory record that changed
     * @param type           the type of change
     * @param quantity       the delta (positive = added, negative = removed)
     * @param quantityBefore stock before the change
     * @param quantityAfter  stock after the change
     * @param performedBy    the user who performed the action (null = system)
     * @param referenceId    optional FK to the triggering entity (order, adjustment, etc.)
     * @param notes          human-readable description of the change
     * @return the persisted InventoryTransaction
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public InventoryTransaction record(
            Inventory inventory,
            TransactionType type,
            int quantity,
            int quantityBefore,
            int quantityAfter,
            User performedBy,
            UUID referenceId,
            String notes) {

        InventoryTransaction transaction = InventoryTransaction.builder()
                .inventory(inventory)
                .product(inventory.getProduct())
                .warehouse(inventory.getWarehouse())
                .performedBy(performedBy)
                .type(type)
                .quantity(quantity)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceId(referenceId)
                .notes(notes)
                .build();

        return transactionRepository.save(transaction);
    }
}
