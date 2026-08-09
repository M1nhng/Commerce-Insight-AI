package com.commerceinsight.customer.dto.response;

import com.commerceinsight.customer.domain.GroupStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerGroupResponse — DTO for customer group data.
 */
public record CustomerGroupResponse(
        UUID id,
        String code,
        String name,
        String description,
        GroupStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
