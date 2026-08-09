package com.commerceinsight.customer.event;

import com.commerceinsight.customer.domain.AddressType;

import java.util.UUID;

/** CustomerAddressChangedEvent — published when a customer's address is added/updated/deleted. */
public record CustomerAddressChangedEvent(
        UUID customerId,
        UUID addressId,
        AddressType addressType,
        String changeType   // "ADDED", "UPDATED", "DELETED", "DEFAULT_SET"
) {}
