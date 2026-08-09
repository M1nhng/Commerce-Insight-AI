package com.commerceinsight.customer.event;

import java.util.UUID;

/**
 * CustomerCreatedEvent — published when a new customer is successfully created.
 *
 * <p>Published via {@link org.springframework.context.ApplicationEventPublisher}.
 * Listeners can react to this event without coupling to the CustomerService.
 */
public record CustomerCreatedEvent(
        UUID customerId,
        String customerCode,
        String email
) {}
