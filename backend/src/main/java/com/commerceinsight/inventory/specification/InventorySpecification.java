package com.commerceinsight.inventory.specification;

import com.commerceinsight.inventory.domain.Inventory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * InventorySpecification — Spring Data JPA Specification predicates for
 * dynamic filtering of Inventory queries.
 */
public final class InventorySpecification {

    private InventorySpecification() {}

    /** Filter by warehouse ID. */
    public static Specification<Inventory> warehouseId(UUID warehouseId) {
        return (root, query, cb) -> {
            if (warehouseId == null) return cb.conjunction();
            return cb.equal(root.get("warehouse").get("id"), warehouseId);
        };
    }

    /** Filter by product ID. */
    public static Specification<Inventory> productId(UUID productId) {
        return (root, query, cb) -> {
            if (productId == null) return cb.conjunction();
            return cb.equal(root.get("product").get("id"), productId);
        };
    }

    /** Filter by product name or SKU (case-insensitive contains). */
    public static Specification<Inventory> productSearch(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) return cb.conjunction();
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("product").get("name")), pattern),
                cb.like(cb.lower(root.get("product").get("sku")), pattern)
            );
        };
    }

    /** Filter to only low-stock items (quantity <= lowStockThreshold). */
    public static Specification<Inventory> isLowStock(Boolean lowStockOnly) {
        return (root, query, cb) -> {
            if (!Boolean.TRUE.equals(lowStockOnly)) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("quantity"), root.get("lowStockThreshold"));
        };
    }

    /** Compose a full inventory filter from individual predicates. */
    public static Specification<Inventory> build(
            UUID warehouseId,
            UUID productId,
            String search,
            Boolean lowStockOnly) {

        return Specification.allOf(
                warehouseId(warehouseId),
                productId(productId),
                productSearch(search),
                isLowStock(lowStockOnly));
    }
}
