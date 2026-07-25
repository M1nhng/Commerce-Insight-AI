package com.commerceinsight.product.repository;

import com.commerceinsight.product.domain.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ProductSpecification — Spring Data JPA Specification for dynamic product filtering.
 *
 * <p>Supports composable predicates for:
 * <ul>
 *   <li>Full-text search on name and SKU (case-insensitive LIKE)</li>
 *   <li>Category ID exact match</li>
 *   <li>Active flag filter</li>
 *   <li>Price range (min/max)</li>
 * </ul>
 *
 * <p>Architecture Rule: Specifications are stateless factory methods.
 * They are composed in the service layer and passed to the repository.
 * No business logic lives here.
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    /**
     * Build a combined Specification from all supplied filter parameters.
     * Null parameters are ignored — they do not restrict the result set.
     *
     * @param search     case-insensitive substring match on name or SKU
     * @param categoryId exact UUID match on category_id
     * @param active     boolean flag filter; null means include both
     * @param priceMin   minimum price (inclusive); null means no lower bound
     * @param priceMax   maximum price (inclusive); null means no upper bound
     */
    public static Specification<Product> of(
            String search,
            UUID categoryId,
            Boolean active,
            BigDecimal priceMin,
            BigDecimal priceMax
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ── Full-text search on name OR sku ─────────────────────────
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")),  pattern)
                ));
            }

            // ── Category filter ──────────────────────────────────────────
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // ── Active flag ──────────────────────────────────────────────
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            // ── Price range ──────────────────────────────────────────────
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
