package com.commerceinsight.customer.domain;

/**
 * CustomerStatus — lifecycle states for a customer record.
 *
 * <p>ACTIVE: Normal operating state. Customer can place orders.
 * <p>INACTIVE: Customer is dormant or voluntarily deactivated.
 * <p>BLOCKED: Customer is blocked due to policy violations; cannot place orders.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED
}
