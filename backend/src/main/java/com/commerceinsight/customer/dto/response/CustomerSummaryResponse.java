package com.commerceinsight.customer.dto.response;

import com.commerceinsight.customer.domain.CustomerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerSummaryResponse — lightweight DTO for paginated list views.
 *
 * <p>Returned by GET /api/v1/customers (paginated list).
 */
public record CustomerSummaryResponse(
        UUID id,
        String customerCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        CustomerStatus status,
        UUID groupId,
        String groupName,
        Instant createdAt
) {}
