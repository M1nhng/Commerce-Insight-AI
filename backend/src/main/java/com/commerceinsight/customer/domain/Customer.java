package com.commerceinsight.customer.domain;

import com.commerceinsight.shared.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer — core entity for customer management.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never expose this entity beyond the service layer. Use DTOs.</li>
 *   <li>Soft delete only — never hard-delete customers (preserve order history).</li>
 *   <li>customerCode must be unique. Email must be unique when provided.</li>
 *   <li>Status transitions: ACTIVE ↔ INACTIVE, ACTIVE → BLOCKED.</li>
 * </ul>
 *
 * <p>Maps to {@code customers} table (extended in V18).
 */
@Entity
@Table(name = "customers")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    /**
     * Unique human-readable code. E.g. "CUST-202607-00042".
     * Generated automatically if not provided.
     */
    @Column(name = "customer_code", nullable = false, length = 50, unique = true)
    private String customerCode;

    /** Customer's given name. Required. */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Customer's family name. Required. */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Optional. Unique when provided (partial unique index in DB). */
    @Column(name = "email", length = 255)
    private String email;

    /** Optional phone number. */
    @Column(name = "phone", length = 50)
    private String phone;

    /** Optional date of birth for demographics. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Optional gender. */
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private CustomerGender gender;

    /**
     * Lifecycle status. Default: ACTIVE.
     * BLOCKED customers cannot place orders.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * Optional customer group assignment.
     * LAZY-loaded for performance.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private CustomerGroup group;

    /**
     * Addresses associated with this customer.
     * LAZY-loaded — only fetch when explicitly needed.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<CustomerAddress> addresses = new ArrayList<>();

    // ── Legacy columns from V5 (kept for DB compatibility, not used in logic) ─

    /**
     * Legacy active flag from V5. Superseded by {@link #status}.
     * Not managed by the application layer — DB default handles it.
     */
    @Column(name = "active", insertable = false, updatable = false)
    private Boolean active;

    // ── Derived helpers ──────────────────────────────────────────────────────

    /** Returns the customer's full name for display. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Returns true if this customer is currently active. */
    public boolean isActive() {
        return CustomerStatus.ACTIVE.equals(status);
    }
}
