package com.commerceinsight.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * CategoryResponse — flat DTO for a single category.
 * Used in list responses and single-record GET.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        UUID parentId,
        int sortOrder,
        boolean active,
        long productCount,
        Instant createdAt
) {}
