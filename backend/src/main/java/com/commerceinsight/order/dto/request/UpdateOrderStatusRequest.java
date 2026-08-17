package com.commerceinsight.order.dto.request;

import com.commerceinsight.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UpdateOrderStatusRequest — payload for status transition.
 *
 * <p>The target status is validated against the allowed-transition policy
 * in {@code OrderStatusTransitionService}.
 */
public record UpdateOrderStatusRequest(

        @NotNull(message = "Target status is required")
        OrderStatus status,

        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason
) {}
