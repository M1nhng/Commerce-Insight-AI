package com.commerceinsight.category.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * UpdateCategoryRequest — validated request body for PUT /api/v1/categories/{id}.
 */
public record UpdateCategoryRequest(

        @NotBlank(message = "Category name must not be blank")
        @Size(max = 150, message = "Category name must not exceed 150 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        /** Null means promote to root category. */
        UUID parentId,

        @Min(value = 0, message = "Sort order must be 0 or greater")
        @Max(value = 9999, message = "Sort order must not exceed 9999")
        Integer sortOrder,

        Boolean active
) {
    public int resolvedSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }

    public boolean resolvedActive() {
        return active == null || active;
    }
}
