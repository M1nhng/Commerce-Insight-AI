package com.commerceinsight.category.repository;

import com.commerceinsight.category.domain.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CategoryRepository — data access for the categories table.
 *
 * <p>All queries automatically exclude soft-deleted categories via
 * {@code @SQLRestriction("deleted_at IS NULL")} on the entity.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Find a category by its slug. Returns empty if not found or soft-deleted.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Check whether a slug is already in use by an active category.
     * Used during create/update to enforce uniqueness before hitting the DB index.
     */
    boolean existsBySlug(String slug);

    /**
     * Check slug uniqueness, excluding a specific category ID (for updates).
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.slug = :slug AND c.id <> :excludeId")
    boolean existsBySlugAndIdNot(@Param("slug") String slug, @Param("excludeId") UUID excludeId);

    /**
     * Find all root-level categories (parent is null), ordered by sort_order.
     */
    List<Category> findByParentIsNullOrderBySortOrderAsc();

    /**
     * Find all direct children of a given parent, ordered by sort_order.
     */
    List<Category> findByParentIdOrderBySortOrderAsc(UUID parentId);

    /**
     * Paginated flat list with optional name search.
     */
    @Query("SELECT c FROM Category c WHERE :search IS NULL OR LOWER(c.name) LIKE :search")
    Page<Category> findAllWithSearch(@Param("search") String search, Pageable pageable);

    /**
     * Count active products in a given category.
     * Used to enforce CATEGORY_HAS_PRODUCTS before soft-delete.
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.deletedAt IS NULL")
    long countActiveProductsByCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Check if any category has the given parent (prevents orphan parent deletion).
     */
    boolean existsByParentId(UUID parentId);

    /** Find category by exact name (case-insensitive). Used by import service. */
    java.util.Optional<Category> findByNameIgnoreCase(String name);
}
