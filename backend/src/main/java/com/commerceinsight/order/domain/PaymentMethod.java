package com.commerceinsight.order.domain;

/**
 * PaymentMethod — how the customer pays for an order.
 * Simulated only — no external gateway integration.
 */
public enum PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD,
    OTHER
}
