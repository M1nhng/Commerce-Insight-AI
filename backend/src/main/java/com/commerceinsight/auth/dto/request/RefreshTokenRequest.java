package com.commerceinsight.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * RefreshTokenRequest — request body for POST /api/v1/auth/refresh.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken

) {}
