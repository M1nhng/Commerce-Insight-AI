package com.commerceinsight.order.dto.response;

import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * OrderSummaryResponse — lightweight order info for list views.
 * Does NOT include items, addresses, or full history — for performance.
 */
public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID customerId,
        String customerName,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal total,
        String currency,
        int itemCount,
        Instant createdAt,
        Instant updatedAt
) {}
