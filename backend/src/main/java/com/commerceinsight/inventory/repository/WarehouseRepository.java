package com.commerceinsight.inventory.repository;

import com.commerceinsight.inventory.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * WarehouseRepository — data access for {@link Warehouse} entities.
 *
 * <p>The {@code @SQLRestriction("deleted_at IS NULL")} on the entity class
 * automatically filters soft-deleted records from all queries.
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID>,
        JpaSpecificationExecutor<Warehouse> {

    /** Check if a warehouse code already exists (for uniqueness validation). */
    boolean existsByCode(String code);

    /** Check if a warehouse code exists, excluding a specific warehouse (for update). */
    boolean existsByCodeAndIdNot(String code, UUID id);

    /** Find a warehouse by its short code. */
    Optional<Warehouse> findByCode(String code);
}
