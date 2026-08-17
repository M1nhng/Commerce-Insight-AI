package com.commerceinsight.order.event;

import java.util.UUID;

/** Published when an order is successfully created (status = PENDING). */
public record OrderCreatedEvent(UUID orderId, String orderNumber, UUID customerId, int itemCount) {}
