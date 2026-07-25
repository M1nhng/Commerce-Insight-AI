package com.commerceinsight.inventory.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * WarehouseResponse — response DTO for warehouse endpoints.
 */
public record WarehouseResponse(
        UUID id,
        String name,
        String code,
        String address,
        String city,
        String country,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
