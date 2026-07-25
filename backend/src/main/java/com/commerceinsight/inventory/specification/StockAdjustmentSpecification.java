package com.commerceinsight.inventory.specification;

import com.commerceinsight.inventory.domain.AdjustmentStatus;
import com.commerceinsight.inventory.domain.StockAdjustment;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * StockAdjustmentSpecification — Spring Data JPA Specification predicates
 * for dynamic filtering of StockAdjustment queries.
 */
public final class StockAdjustmentSpecification {

    private StockAdjustmentSpecification() {}

    /** Filter by adjustment status. */
    public static Specification<StockAdjustment> status(AdjustmentStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    /** Filter by warehouse ID. */
    public static Specification<StockAdjustment> warehouseId(UUID warehouseId) {
        return (root, query, cb) -> {
            if (warehouseId == null) return cb.conjunction();
            return cb.equal(root.get("warehouse").get("id"), warehouseId);
        };
    }

    /** Filter by product ID. */
    public static Specification<StockAdjustment> productId(UUID productId) {
        return (root, query, cb) -> {
            if (productId == null) return cb.conjunction();
            return cb.equal(root.get("product").get("id"), productId);
        };
    }

    /** Filter by the requesting user. */
    public static Specification<StockAdjustment> requestedBy(UUID userId) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            return cb.equal(root.get("requestedBy").get("id"), userId);
        };
    }

    /** Compose a full stock adjustment filter from individual predicates. */
    public static Specification<StockAdjustment> build(
            AdjustmentStatus status,
            UUID warehouseId,
            UUID productId,
            UUID requestedBy) {

        return Specification.allOf(
                status(status),
                warehouseId(warehouseId),
                productId(productId),
                requestedBy(requestedBy));
    }
}
