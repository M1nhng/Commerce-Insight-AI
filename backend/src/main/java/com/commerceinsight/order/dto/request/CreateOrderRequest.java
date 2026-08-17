package com.commerceinsight.order.dto.request;

import com.commerceinsight.order.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * CreateOrderRequest — full order creation payload.
 *
 * <p>Backend rules:
 * <ul>
 *   <li>Totals are NEVER trusted from the client — backend recalculates.</li>
 *   <li>At least one item required.</li>
 *   <li>Customer must exist and be ACTIVE.</li>
 *   <li>All products must exist and be active.</li>
 *   <li>Inventory must be sufficient for every item.</li>
 * </ul>
 */
public record CreateOrderRequest(

        @NotNull(message = "Customer ID is required")
        UUID customerId,

        @NotNull(message = "At least one item is required")
        @Size(min = 1, max = 100, message = "Order must have between 1 and 100 items")
        @Valid
        List<CreateOrderItemRequest> items,

        /** Optional shipping address snapshot. */
        @Valid
        CreateOrderAddressRequest shippingAddress,

        /** Optional billing address snapshot. */
        @Valid
        CreateOrderAddressRequest billingAddress,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        /** Shipping fee in VND. Defaults to 0 if null. */
        @DecimalMin(value = "0.0", inclusive = true, message = "Shipping fee cannot be negative")
        BigDecimal shippingFee,

        /** Order-level discount. Defaults to 0 if null. */
        @DecimalMin(value = "0.0", inclusive = true, message = "Discount cannot be negative")
        BigDecimal discount,

        /** Tax amount. Defaults to 0 if null. */
        @DecimalMin(value = "0.0", inclusive = true, message = "Tax cannot be negative")
        BigDecimal tax,

        /** Currency code. Defaults to VND. */
        @Size(max = 10)
        String currency,

        /** Free-text notes. */
        @Size(max = 2000, message = "Notes must not exceed 2000 characters")
        String notes
) {}
