package com.commerceinsight.customer.event;

import java.util.UUID;

/** CustomerUpdatedEvent — published when a customer's profile is updated. */
public record CustomerUpdatedEvent(UUID customerId, String customerCode) {}
