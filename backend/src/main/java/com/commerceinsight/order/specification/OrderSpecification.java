package com.commerceinsight.order.specification;

import com.commerceinsight.order.domain.Order;
import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * OrderSpecification — JPA Specification builder for dynamic order filtering.
 *
 * <p>Supports: keyword (orderNumber), customerId, status, paymentStatus, dateFrom, dateTo.
 */
public class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> build(
            String keyword,
            UUID customerId,
            OrderStatus status,
            PaymentStatus paymentStatus,
            Instant dateFrom,
            Instant dateTo) {

        return Specification
                .where(hasKeyword(keyword))
                .and(hasCustomer(customerId))
                .and(hasStatus(status))
                .and(hasPaymentStatus(paymentStatus))
                .and(createdAfter(dateFrom))
                .and(createdBefore(dateTo));
    }

    private static Specification<Order> hasKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("orderNumber")), pattern);
    }

    private static Specification<Order> hasCustomer(UUID customerId) {
        if (customerId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    private static Specification<Order> hasStatus(OrderStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Order> hasPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) return null;
        return (root, query, cb) -> cb.equal(root.get("paymentStatus"), paymentStatus);
    }

    private static Specification<Order> createdAfter(Instant dateFrom) {
        if (dateFrom == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom);
    }

    private static Specification<Order> createdBefore(Instant dateTo) {
        if (dateTo == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), dateTo);
    }
}
