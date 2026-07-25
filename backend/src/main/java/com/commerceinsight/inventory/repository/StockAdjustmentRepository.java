package com.commerceinsight.inventory.repository;

import com.commerceinsight.inventory.domain.AdjustmentStatus;
import com.commerceinsight.inventory.domain.StockAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * StockAdjustmentRepository — data access for {@link StockAdjustment} entities.
 */
@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID>,
        JpaSpecificationExecutor<StockAdjustment> {

    /** Paginated adjustments for a specific inventory record. */
    Page<StockAdjustment> findAllByInventoryIdOrderByCreatedAtDesc(UUID inventoryId, Pageable pageable);

    /** Paginated adjustments filtered by status. */
    Page<StockAdjustment> findAllByStatusOrderByCreatedAtDesc(AdjustmentStatus status, Pageable pageable);

    /** Pending adjustments awaiting review — used for dashboard alerts. */
    long countByStatus(AdjustmentStatus status);
}
