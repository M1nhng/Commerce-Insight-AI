package com.commerceinsight.customer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerAddress — a shipping or billing address for a customer.
 *
 * <p>Business Rules (enforced in service layer):
 * <ul>
 *   <li>A customer may have at most ONE default SHIPPING address.</li>
 *   <li>A customer may have at most ONE default BILLING address.</li>
 *   <li>Setting a new default automatically clears the previous default of the same type.</li>
 * </ul>
 *
 * <p>Maps to {@code customer_addresses} table.
 */
@Entity
@Table(name = "customer_addresses")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * The customer this address belongs to.
     * LAZY-loaded.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Address type: SHIPPING or BILLING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AddressType type;

    /**
     * Name of the person who will receive the delivery at this address.
     */
    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    /** Contact phone for this address. */
    @Column(name = "phone", length = 50)
    private String phone;

    /** Street address / house number. */
    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    /** Ward (phường/xã). */
    @Column(name = "ward", length = 150)
    private String ward;

    /** District (quận/huyện). */
    @Column(name = "district", length = 150)
    private String district;

    /** Province/city (tỉnh/thành). */
    @Column(name = "province", length = 150)
    private String province;

    /** Country code (default: VN). */
    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "VN";

    /**
     * Whether this is the default address for its type.
     * Invariant: at most one default per (customer, type).
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
