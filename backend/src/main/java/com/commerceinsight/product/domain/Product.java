package com.commerceinsight.product.domain;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Product — the core product catalog entity.
 *
 * <p>Maps to the {@code products} table.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never expose this entity beyond the service layer. Use DTOs.</li>
 *   <li>Soft delete: {@code @SQLRestriction("deleted_at IS NULL")}.</li>
 *   <li>SKU uniqueness is enforced at DB level (partial index) and at service level.</li>
 * </ul>
 */
@Entity
@Table(name = "products")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    /**
     * Stock-keeping unit — unique identifier within the catalogue.
     * Uniqueness enforced by partial index {@code uq_products_sku (deleted_at IS NULL)}.
     */
    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    /** Human-readable product name. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Optional rich text description. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Selling price. DECIMAL(19,4) — never use {@code double} for money.
     * Must be >= 0 (enforced at DB level with CHECK constraint).
     */
    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    /**
     * Optional cost price for margin calculation.
     * May be null if the cost is unknown.
     */
    @Column(name = "cost_price", precision = 19, scale = 4)
    private BigDecimal costPrice;

    /** Primary image URL. External URL stored as-is. */
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /**
     * Category this product belongs to. Nullable — a product without a category
     * is still valid (ON DELETE SET NULL at DB level).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Whether the product is active (visible to users).
     * Inactive products are hidden from list endpoints by default.
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Additional product images (beyond the primary imageUrl).
     * Cascade ALL so images are removed when the product is hard-deleted in tests.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();
}
