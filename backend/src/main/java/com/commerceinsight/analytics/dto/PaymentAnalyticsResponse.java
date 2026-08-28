package com.commerceinsight.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * PaymentAnalyticsResponse — payment breakdown grouped by method.
 *
 * <p>The {@code breakdown} map keys are {@link com.commerceinsight.order.domain.PaymentMethod}
 * string values: {@code CASH}, {@code BANK_TRANSFER}, {@code CARD}, {@code OTHER}.
 * Missing methods are omitted from the map (i.e. no zero-count entries).
 */
@Schema(description = "Payment statistics grouped by payment method")
public record PaymentAnalyticsResponse(

        @Schema(description = "ISO 4217 currency code for all monetary values")
        String currency,

        @Schema(
                description = "Map of PaymentMethod → stats. Keys: CASH, BANK_TRANSFER, CARD, OTHER",
                example = "{\"CASH\":{\"orders\":20,\"amount\":15000},\"CARD\":{\"orders\":45,\"amount\":48000}}"
        )
        Map<String, PaymentMethodStats> breakdown,

        @Schema(description = "Start of the reporting window (null = all-time)")
        Instant dateFrom,

        @Schema(description = "End of the reporting window (null = now)")
        Instant dateTo
) {}
