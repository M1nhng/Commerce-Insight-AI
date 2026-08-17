package com.commerceinsight.order.event;

import java.util.UUID;

/** Published when order reaches COMPLETED (final happy-path terminal state). */
public record OrderCompletedEvent(UUID orderId, String orderNumber) {}
