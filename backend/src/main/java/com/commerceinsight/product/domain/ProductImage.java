package com.commerceinsight.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * ProductImage — additional images for a product beyond the primary {@code image_url}.
 *
 * <p>Maps to the {@code product_images} table (V13 migration).
 *
 * <p>Does NOT extend BaseEntity intentionally — images have no soft-delete lifecycle;
 * they are fully owned by the product and cascade-deleted with it.
 */
@Entity
@Table(name = "product_images")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Owning product. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** External image URL. */
    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    /** Accessibility alt text for the image. */
    @Column(name = "alt_text", length = 255)
    private String altText;

    /** Display order within the product's image gallery. Lower = first. */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
