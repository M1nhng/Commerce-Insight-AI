package com.commerceinsight.category.domain;

import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * Category — product category entity supporting unlimited tree depth.
 *
 * <p>Maps to the {@code categories} table.
 *
 * <p>Tree structure: a category with {@code parentId == null} is a root category.
 * Categories can have any number of children. Deleting a category that still has
 * active products is rejected at the service layer (CATEGORY_HAS_PRODUCTS).
 *
 * <p>Soft delete: {@code @SQLRestriction("deleted_at IS NULL")} hides deleted
 * categories from all JPA queries automatically.
 */
@Entity
@Table(name = "categories")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    /**
     * Human-readable category name. Must be non-blank, max 150 chars.
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * URL-friendly slug — auto-generated from name, unique among active categories.
     * Enforced by partial unique index {@code uq_categories_slug}.
     */
    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    /**
     * Optional description text.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Parent category. Null indicates a root category.
     * FK: categories.parent_id → categories.id ON DELETE RESTRICT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Children of this category. Not fetched by default — loaded explicitly when
     * building the tree structure.
     */
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    /**
     * Display order within siblings. Lower = displayed first.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    /**
     * Whether this category is active (visible to the storefront).
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
