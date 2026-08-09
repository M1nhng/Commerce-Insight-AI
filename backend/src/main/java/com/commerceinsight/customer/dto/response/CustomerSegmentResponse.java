package com.commerceinsight.customer.dto.response;

import com.commerceinsight.customer.domain.GroupStatus;
import com.commerceinsight.customer.domain.SegmentType;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerSegmentResponse — DTO for customer segment data.
 */
public record CustomerSegmentResponse(
        UUID id,
        String code,
        String name,
        String description,
        SegmentType type,
        GroupStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
