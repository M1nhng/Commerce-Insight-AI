package com.commerceinsight.product.repository;

import com.commerceinsight.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ProductRepository — data access for the products table.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@link ProductSpecification}.
 *
 * <p>All queries automatically exclude soft-deleted products via
 * {@code @SQLRestriction("deleted_at IS NULL")} on the entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    /**
     * Check if a SKU is already in use by an active (non-deleted) product.
     */
    boolean existsBySku(String sku);

    /**
     * Check SKU uniqueness, excluding a specific product ID (for updates).
     */
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.id <> :excludeId")
    boolean existsBySkuAndIdNot(@Param("sku") String sku, @Param("excludeId") UUID excludeId);

    /**
     * Count active products in a given category.
     * Used by CategoryService to enforce CATEGORY_HAS_PRODUCTS.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") UUID categoryId);
}
