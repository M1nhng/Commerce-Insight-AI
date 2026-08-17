package com.commerceinsight.order.event;

import java.util.UUID;

/** Published when order transitions to DELIVERED. */
public record OrderDeliveredEvent(UUID orderId, String orderNumber) {}
