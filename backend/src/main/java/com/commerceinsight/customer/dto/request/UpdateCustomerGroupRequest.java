package com.commerceinsight.customer.dto.request;

import com.commerceinsight.customer.domain.GroupStatus;
import jakarta.validation.constraints.Size;

/**
 * UpdateCustomerGroupRequest — payload for PUT /api/v1/customer-groups/{id}.
 *
 * <p>All fields are optional. Only non-null values are applied.
 */
public record UpdateCustomerGroupRequest(

        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        String description,

        GroupStatus status
) {}
