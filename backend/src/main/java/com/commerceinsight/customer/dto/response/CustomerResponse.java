package com.commerceinsight.customer.dto.response;

import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.domain.CustomerGender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CustomerResponse — full customer detail DTO.
 *
 * <p>Returned by GET /api/v1/customers/{id} and POST/PUT operations.
 */
public record CustomerResponse(
        UUID id,
        String customerCode,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        CustomerGender gender,
        CustomerStatus status,
        UUID groupId,
        String groupName,
        List<CustomerAddressResponse> addresses,
        Instant createdAt,
        Instant updatedAt
) {}
