package com.commerceinsight.customer.dto.request;

import com.commerceinsight.customer.domain.GroupStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CreateCustomerGroupRequest — payload for POST /api/v1/customer-groups.
 */
public record CreateCustomerGroupRequest(

        @NotBlank(message = "Group code is required")
        @Size(max = 50, message = "Code must be at most 50 characters")
        String code,

        @NotBlank(message = "Group name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        String description,

        GroupStatus status
) {}
