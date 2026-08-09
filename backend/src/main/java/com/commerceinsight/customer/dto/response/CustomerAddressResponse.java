package com.commerceinsight.customer.dto.response;

import com.commerceinsight.customer.domain.AddressType;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerAddressResponse — DTO for a single customer address.
 */
public record CustomerAddressResponse(
        UUID id,
        UUID customerId,
        AddressType type,
        String recipientName,
        String phone,
        String addressLine,
        String ward,
        String district,
        String province,
        String country,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {}
