package com.commerceinsight.order.dto.response;

import com.commerceinsight.order.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * OrderStatusHistoryResponse — single entry in the status audit trail.
 */
public record OrderStatusHistoryResponse(
        UUID id,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        UUID changedById,
        String changedByName,
        String reason,
        Instant createdAt
) {}
