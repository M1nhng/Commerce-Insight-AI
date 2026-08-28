package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * PaymentMethodStats — aggregated stats for one payment method.
 */
@Schema(description = "Aggregated order count and total amount for one payment method")
public record PaymentMethodStats(

        @Schema(description = "Number of orders using this payment method")
        long orders,

        @Schema(description = "Sum of payment amounts for this method")
        BigDecimal amount
) {}
