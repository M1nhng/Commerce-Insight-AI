package com.commerceinsight.dataimport.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * ImportErrorResponse — single row error details for the errors pagination endpoint.
 */
@Builder
public record ImportErrorResponse(

        UUID id,
        int rowNumber,
        String fieldName,
        String rawValue,
        String errorCode,
        String errorMessage
) {}
