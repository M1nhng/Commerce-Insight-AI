package com.commerceinsight.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * OrderAddress — immutable address snapshot for an order.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>Captured at order creation time and NEVER modified afterward.</li>
 *   <li>Changing a customer's address after ordering must NOT affect these records.</li>
 *   <li>No BaseEntity — no updatedAt, no deletedAt. Purposely immutable.</li>
 *   <li>No soft delete — cascades with parent order.</li>
 * </ul>
 *
 * <p>Maps to the {@code order_addresses} table (created in V24).
 */
@Entity
@Table(name = "order_addresses")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAddress implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Parent order. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Whether this is a SHIPPING or BILLING address. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private OrderAddressType type;

    /** Name of the recipient at this address. */
    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    /** Contact phone for delivery. */
    @Column(name = "phone", length = 50)
    private String phone;

    /** Full street address. */
    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    /** Phường / Xã. */
    @Column(name = "ward", length = 100)
    private String ward;

    /** Quận / Huyện. */
    @Column(name = "district", length = 100)
    private String district;

    /** Tỉnh / Thành phố. */
    @Column(name = "province", length = 100)
    private String province;

    /** Country (default: Vietnam). */
    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "Vietnam";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
