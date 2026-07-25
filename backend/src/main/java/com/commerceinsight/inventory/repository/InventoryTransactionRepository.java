package com.commerceinsight.inventory.repository;

import com.commerceinsight.inventory.domain.InventoryTransaction;
import com.commerceinsight.inventory.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * InventoryTransactionRepository — data access for {@link InventoryTransaction} records.
 *
 * <p>Transactions are immutable — no update or delete methods are exposed.
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID>,
        JpaSpecificationExecutor<InventoryTransaction> {

    /** Paginated transaction history for a specific inventory record. */
    Page<InventoryTransaction> findAllByInventoryIdOrderByCreatedAtDesc(UUID inventoryId, Pageable pageable);

    /** Paginated transaction history for a specific product (all warehouses). */
    Page<InventoryTransaction> findAllByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    /** Paginated transaction history for a specific warehouse. */
    Page<InventoryTransaction> findAllByWarehouseIdOrderByCreatedAtDesc(UUID warehouseId, Pageable pageable);

    /** Paginated transaction history filtered by type. */
    Page<InventoryTransaction> findAllByInventoryIdAndTypeOrderByCreatedAtDesc(
            UUID inventoryId, TransactionType type, Pageable pageable);
}
