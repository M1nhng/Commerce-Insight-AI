package com.commerceinsight.order.dto.request;

import com.commerceinsight.order.domain.OrderAddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CreateOrderAddressRequest — address snapshot input for order creation.
 */
public record CreateOrderAddressRequest(

        @NotNull(message = "Address type is required (SHIPPING or BILLING)")
        OrderAddressType type,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 255, message = "Recipient name must not exceed 255 characters")
        String recipientName,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone,

        @NotBlank(message = "Address line is required")
        @Size(max = 500, message = "Address line must not exceed 500 characters")
        String addressLine,

        @Size(max = 100) String ward,
        @Size(max = 100) String district,
        @Size(max = 100) String province,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country
) {}
