package com.commerceinsight.customer.dto.request;

import com.commerceinsight.customer.domain.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * CreateAddressRequest — payload for POST /api/v1/customers/{id}/addresses.
 */
public record CreateAddressRequest(

        @NotNull(message = "Address type is required (SHIPPING or BILLING)")
        AddressType type,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 200, message = "Recipient name must be at most 200 characters")
        String recipientName,

        @Pattern(regexp = "^[+]?[\\d\\s\\-().]{7,20}$",
                 message = "Phone must be a valid phone number")
        String phone,

        @NotBlank(message = "Address line is required")
        @Size(max = 500, message = "Address line must be at most 500 characters")
        String addressLine,

        @Size(max = 150) String ward,
        @Size(max = 150) String district,
        @Size(max = 150) String province,

        @Size(max = 100, message = "Country must be at most 100 characters")
        String country,

        boolean isDefault
) {}
