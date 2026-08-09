package com.commerceinsight.customer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerSegment — foundation entity for customer segmentation.
 *
 * <p>This sprint only establishes the schema and entity.
 * Segment assignment to customers and rule-based/AI evaluation
 * are deferred to the Analytics sprint.
 *
 * <p>Maps to {@code customer_segments} table.
 */
@Entity
@Table(name = "customer_segments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Unique code, e.g. "HIGH_VALUE", "AT_RISK". */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /** Human-readable name. */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Optional description of what this segment represents. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Segment classification type.
     * Only MANUAL is used in this sprint.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    @Builder.Default
    private SegmentType type = SegmentType.MANUAL;

    /**
     * Segment lifecycle status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GroupStatus status = GroupStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
