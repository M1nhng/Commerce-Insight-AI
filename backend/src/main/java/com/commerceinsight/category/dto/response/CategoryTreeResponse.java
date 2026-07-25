package com.commerceinsight.category.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CategoryTreeResponse — hierarchical DTO for GET /api/v1/categories/tree.
 *
 * <p>Children are recursively nested. Leaf nodes have an empty {@code children} list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryTreeResponse(
        UUID id,
        String name,
        String slug,
        String description,
        UUID parentId,
        int sortOrder,
        boolean active,
        long productCount,
        Instant createdAt,
        List<CategoryTreeResponse> children
) {}
