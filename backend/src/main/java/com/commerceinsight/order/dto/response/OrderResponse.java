package com.commerceinsight.order.dto.response;

import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * OrderResponse — full order detail including items, addresses, payment and status history.
 */
public record OrderResponse(
        UUID id,
        String orderNumber,

        // Customer summary
        UUID customerId,
        String customerName,
        String customerCode,

        // Status
        OrderStatus status,
        PaymentStatus paymentStatus,

        // Financials
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal tax,
        BigDecimal total,
        String currency,

        // Timestamps
        Instant createdAt,
        Instant updatedAt,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        Instant completedAt,

        // Notes
        String notes,

        // Relations
        List<OrderItemResponse> items,
        OrderAddressResponse shippingAddress,
        OrderAddressResponse billingAddress,
        PaymentResponse payment,
        List<OrderStatusHistoryResponse> statusHistory
) {}
