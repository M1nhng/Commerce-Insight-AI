package com.commerceinsight.customer.event;

import com.commerceinsight.customer.domain.CustomerStatus;

import java.util.UUID;

/**
 * CustomerStatusChangedEvent — published when a customer's status changes.
 *
 * <p>Useful for downstream systems: e.g., notifying order service when
 * a customer is BLOCKED so that pending orders can be reviewed.
 */
public record CustomerStatusChangedEvent(
        UUID customerId,
        String customerCode,
        CustomerStatus oldStatus,
        CustomerStatus newStatus
) {}
