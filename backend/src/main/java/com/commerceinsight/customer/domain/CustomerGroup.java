package com.commerceinsight.customer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerGroup — a named group that customers can be assigned to.
 *
 * <p>Examples: VIP, WHOLESALE, RETAIL, PREMIUM.
 *
 * <p>Architecture Rule: No soft delete on groups. Deleting a group clears
 * group_id on associated customers (SET NULL enforced at DB level via FK).
 *
 * <p>Maps to {@code customer_groups} table.
 */
@Entity
@Table(name = "customer_groups")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Unique short code. E.g. "VIP", "WHOLESALE".
     */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /**
     * Human-readable group name.
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Optional description of the group's purpose.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Group lifecycle status.
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
