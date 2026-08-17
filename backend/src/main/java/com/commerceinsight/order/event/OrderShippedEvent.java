package com.commerceinsight.order.event;

import java.util.UUID;

/** Published when order transitions to SHIPPED (inventory stock-out committed). */
public record OrderShippedEvent(UUID orderId, String orderNumber) {}
