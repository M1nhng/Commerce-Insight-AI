package com.commerceinsight.order.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * OrderStatus — all possible states of an order.
 *
 * <p>State machine (explicit transitions enforced by {@code OrderStatusTransitionService}):
 * <pre>
 *   PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → COMPLETED
 *   PENDING  → CANCELLED
 *   CONFIRMED → CANCELLED
 *   PROCESSING → CANCELLED
 * </pre>
 *
 * <p>SHIPPED, DELIVERED, COMPLETED cannot be cancelled.
 * REFUNDED is a terminal state only reachable by future refund logic.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUNDED;

    /**
     * Returns the set of statuses this status can legally transition to.
     */
    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING    -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED  -> EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED    -> EnumSet.of(DELIVERED);
            case DELIVERED  -> EnumSet.of(COMPLETED);
            case COMPLETED,
                 CANCELLED,
                 REFUNDED   -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    /**
     * Returns true if this status can be cancelled.
     */
    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED || this == PROCESSING;
    }

    /**
     * Returns true if this is a terminal (no further transitions) status.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }
}
