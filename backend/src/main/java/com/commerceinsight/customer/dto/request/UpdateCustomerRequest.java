package com.commerceinsight.customer.dto.request;

import com.commerceinsight.customer.domain.CustomerGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * UpdateCustomerRequest — payload for PUT /api/v1/customers/{id}.
 *
 * <p>All fields are optional. Only non-null values are applied.
 */
public record UpdateCustomerRequest(

        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Pattern(regexp = "^[+]?[\\d\\s\\-().]{7,20}$",
                 message = "Phone must be a valid phone number")
        String phone,

        LocalDate dateOfBirth,

        CustomerGender gender,

        UUID groupId
) {}
