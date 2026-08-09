package com.commerceinsight.customer.dto.request;

import com.commerceinsight.customer.domain.CustomerStatus;
import jakarta.validation.constraints.NotNull;

/**
 * UpdateCustomerStatusRequest — payload for PATCH /api/v1/customers/{id}/status.
 */
public record UpdateCustomerStatusRequest(

        @NotNull(message = "Status is required")
        CustomerStatus status
) {}
