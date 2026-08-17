package com.commerceinsight.order.dto.response;

import com.commerceinsight.order.domain.OrderAddressType;

import java.time.Instant;
import java.util.UUID;

/**
 * OrderAddressResponse — immutable address snapshot in order responses.
 */
public record OrderAddressResponse(
        UUID id,
        OrderAddressType type,
        String recipientName,
        String phone,
        String addressLine,
        String ward,
        String district,
        String province,
        String country,
        Instant createdAt
) {}
