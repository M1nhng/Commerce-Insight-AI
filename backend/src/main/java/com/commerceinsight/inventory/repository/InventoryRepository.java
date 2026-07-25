package com.commerceinsight.inventory.repository;

import com.commerceinsight.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * InventoryRepository — data access for {@link Inventory} entities.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID>,
        JpaSpecificationExecutor<Inventory> {

    /** Find inventory for a specific product + warehouse combination. */
    Optional<Inventory> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId);

    /** Find all inventory entries for a given product (across all warehouses). */
    List<Inventory> findAllByProductId(UUID productId);

    /** Find all inventory entries for a given warehouse. */
    List<Inventory> findAllByWarehouseId(UUID warehouseId);

    /** Check if an inventory row already exists for the given product/warehouse pair. */
    boolean existsByProductIdAndWarehouseId(UUID productId, UUID warehouseId);

    /**
     * Find all inventory records where quantity is at or below the low stock threshold.
     * Used by the low-stock alert endpoint.
     */
    @Query("""
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            WHERE i.quantity <= i.lowStockThreshold
            ORDER BY i.quantity ASC
            """)
    List<Inventory> findLowStockItems();

    /**
     * Find all inventory records where quantity is at or below the given threshold,
     * for a specific warehouse.
     */
    @Query("""
            SELECT i FROM Inventory i
            JOIN FETCH i.product p
            JOIN FETCH i.warehouse w
            WHERE i.warehouse.id = :warehouseId
              AND i.quantity <= i.lowStockThreshold
            ORDER BY i.quantity ASC
            """)
    List<Inventory> findLowStockItemsByWarehouse(@Param("warehouseId") UUID warehouseId);
}
