package com.commerceinsight.customer.dto.request;

import com.commerceinsight.customer.domain.CustomerGender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * CreateCustomerRequest — payload for POST /api/v1/customers.
 *
 * <p>customerCode is optional: if blank, the service auto-generates one.
 * email is optional but must be a valid email if provided.
 * phone is optional but must match the allowed pattern if provided.
 */
public record CreateCustomerRequest(

        @Size(max = 50, message = "Customer code must be at most 50 characters")
        String customerCode,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
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
