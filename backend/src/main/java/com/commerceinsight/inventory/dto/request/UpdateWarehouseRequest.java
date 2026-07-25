package com.commerceinsight.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UpdateWarehouseRequest — request body for PUT /api/v1/warehouses/{id}.
 */
public record UpdateWarehouseRequest(

        @NotBlank(message = "Warehouse name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Warehouse code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @Size(max = 2000, message = "Address must not exceed 2000 characters")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        boolean active
) {}
