package com.commerceinsight.order.mapper;

import com.commerceinsight.order.domain.*;
import com.commerceinsight.order.dto.response.*;
import org.mapstruct.*;

import java.util.List;

/**
 * OrderMapper — MapStruct mapper between Order entities and DTOs.
 *
 * <p>Architecture rule: Never expose entities beyond the service layer.
 * All mapping from entity → DTO happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    // ── OrderItem ─────────────────────────────────────────────────────────────

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    // ── OrderAddress ──────────────────────────────────────────────────────────

    OrderAddressResponse toAddressResponse(OrderAddress address);

    // ── Payment ───────────────────────────────────────────────────────────────

    @Mapping(target = "createdAt", source = "createdAt")
    PaymentResponse toPaymentResponse(Payment payment);

    // ── OrderStatusHistory ────────────────────────────────────────────────────

    @Mapping(target = "changedById",   source = "changedBy.id")
    @Mapping(target = "changedByName", expression = "java(history.getChangedBy() != null ? history.getChangedBy().getFirstName() + \" \" + history.getChangedBy().getLastName() : null)")
    OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history);

    List<OrderStatusHistoryResponse> toHistoryResponseList(List<OrderStatusHistory> history);

    // ── Order Summary (list view) ─────────────────────────────────────────────

    @Mapping(target = "customerId",   source = "customer.id")
    @Mapping(target = "customerName", expression = "java(order.getCustomer() != null ? order.getCustomer().getFirstName() + \" \" + order.getCustomer().getLastName() : \"Unknown\")")
    @Mapping(target = "itemCount",    expression = "java(order.getItems().size())")
    OrderSummaryResponse toSummary(Order order);

    // ── Order Full Detail ─────────────────────────────────────────────────────

    @Mapping(target = "customerId",       source = "customer.id")
    @Mapping(target = "customerName",     expression = "java(order.getCustomer() != null ? order.getCustomer().getFirstName() + \" \" + order.getCustomer().getLastName() : \"Unknown\")")
    @Mapping(target = "customerCode",     source = "customer.customerCode")
    @Mapping(target = "items",            source = "items")
    @Mapping(target = "payment",          source = "payment")
    @Mapping(target = "statusHistory",    source = "statusHistory")
    @Mapping(target = "shippingAddress",  expression = "java(resolveAddress(order, com.commerceinsight.order.domain.OrderAddressType.SHIPPING))")
    @Mapping(target = "billingAddress",   expression = "java(resolveAddress(order, com.commerceinsight.order.domain.OrderAddressType.BILLING))")
    OrderResponse toResponse(Order order);

    // ── Helper ────────────────────────────────────────────────────────────────

    default OrderAddressResponse resolveAddress(Order order, OrderAddressType type) {
        return order.getAddresses().stream()
                .filter(a -> a.getType() == type)
                .findFirst()
                .map(this::toAddressResponse)
                .orElse(null);
    }
}
