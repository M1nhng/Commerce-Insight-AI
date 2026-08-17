package com.commerceinsight.order.event;

import java.util.UUID;

/** Published when an order transitions from PENDING to CONFIRMED. */
public record OrderConfirmedEvent(UUID orderId, String orderNumber) {}
