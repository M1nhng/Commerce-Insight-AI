package com.commerceinsight.inventory.domain;

import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * Warehouse — a named physical or virtual stock location.
 *
 * <p>Maps to the {@code warehouses} table.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never expose this entity beyond the service layer. Use DTOs.</li>
 *   <li>Soft delete: {@code @SQLRestriction("deleted_at IS NULL")}.</li>
 *   <li>Code uniqueness enforced by partial index {@code uq_warehouses_code (deleted_at IS NULL)}.</li>
 * </ul>
 */
@Entity
@Table(name = "warehouses")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse extends BaseEntity {

    /**
     * Human-readable name for the warehouse location.
     * Example: "Main Warehouse", "East Coast Distribution Center"
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Short unique code used in operational contexts.
     * Example: "WH-MAIN", "WH-EAST"
     * Uniqueness enforced by partial index {@code uq_warehouses_code}.
     */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** Optional street address or location description. */
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    /** Optional city where the warehouse is located. */
    @Column(name = "city", length = 100)
    private String city;

    /** Optional country where the warehouse is located. */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Whether this warehouse is active.
     * Inactive warehouses cannot receive new stock operations.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
