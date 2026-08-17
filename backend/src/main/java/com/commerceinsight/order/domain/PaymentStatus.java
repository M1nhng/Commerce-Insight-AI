package com.commerceinsight.order.domain;

/**
 * PaymentStatus — lifecycle of a payment record.
 *
 * <ul>
 *   <li>PENDING — payment record created but not yet confirmed</li>
 *   <li>PAID — payment confirmed</li>
 *   <li>FAILED — payment attempt failed</li>
 *   <li>REFUNDED — payment reversed (future use)</li>
 * </ul>
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
