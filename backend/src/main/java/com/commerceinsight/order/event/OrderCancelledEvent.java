package com.commerceinsight.order.event;

import java.util.UUID;

/** Published when an order is cancelled (inventory reservation released). */
public record OrderCancelledEvent(UUID orderId, String orderNumber, String reason) {}
