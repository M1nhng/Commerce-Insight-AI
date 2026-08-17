package com.commerceinsight.order.dto.response;

import com.commerceinsight.order.domain.PaymentMethod;
import com.commerceinsight.order.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * PaymentResponse — payment info within an order detail response.
 */
public record PaymentResponse(
        UUID id,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String reference,
        Instant paidAt,
        String notes,
        Instant createdAt
) {}
