package com.commerceinsight.order.dto.request;

import jakarta.validation.constraints.Size;

/**
 * CancelOrderRequest — optional reason when cancelling an order.
 */
public record CancelOrderRequest(

        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason
) {}
