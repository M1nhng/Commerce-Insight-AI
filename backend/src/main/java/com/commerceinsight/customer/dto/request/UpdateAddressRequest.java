package com.commerceinsight.customer.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * UpdateAddressRequest — payload for PUT /api/v1/customers/{id}/addresses/{addressId}.
 *
 * <p>All fields are optional. Only non-null values are applied.
 */
public record UpdateAddressRequest(

        @Size(max = 200, message = "Recipient name must be at most 200 characters")
        String recipientName,

        @Pattern(regexp = "^[+]?[\\d\\s\\-().]{7,20}$",
                 message = "Phone must be a valid phone number")
        String phone,

        @Size(max = 500, message = "Address line must be at most 500 characters")
        String addressLine,

        @Size(max = 150) String ward,
        @Size(max = 150) String district,
        @Size(max = 150) String province,
        @Size(max = 100) String country
) {}
