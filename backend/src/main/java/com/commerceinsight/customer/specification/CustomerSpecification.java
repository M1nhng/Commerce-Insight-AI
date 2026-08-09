package com.commerceinsight.customer.specification;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * CustomerSpecification — Spring Data JPA Specification predicates for
 * dynamic filtering of Customer queries.
 *
 * <p>Keyword search covers: customerCode, firstName, lastName, email, phone.
 */
public final class CustomerSpecification {

    private CustomerSpecification() {}

    /** Keyword search — case-insensitive LIKE across multiple fields. */
    public static Specification<Customer> keyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("customerCode")), pattern),
                cb.like(cb.lower(root.get("firstName")),    pattern),
                cb.like(cb.lower(root.get("lastName")),     pattern),
                cb.like(cb.lower(root.get("email")),        pattern),
                cb.like(cb.lower(root.get("phone")),        pattern)
            );
        };
    }

    /** Filter by customer status. */
    public static Specification<Customer> status(CustomerStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("status"), status);
        };
    }

    /** Filter by customer group. */
    public static Specification<Customer> groupId(UUID groupId) {
        return (root, query, cb) -> {
            if (groupId == null) return cb.conjunction();
            return cb.equal(root.get("group").get("id"), groupId);
        };
    }

    /** Filter by createdAt >= startDate. */
    public static Specification<Customer> createdFrom(Instant startDate) {
        return (root, query, cb) -> {
            if (startDate == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
        };
    }

    /** Filter by createdAt <= endDate. */
    public static Specification<Customer> createdTo(Instant endDate) {
        return (root, query, cb) -> {
            if (endDate == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
        };
    }

    /** Compose all predicates. */
    public static Specification<Customer> build(
            String keyword,
            CustomerStatus status,
            UUID groupId,
            Instant startDate,
            Instant endDate) {

        return Specification.allOf(
                keyword(keyword),
                status(status),
                groupId(groupId),
                createdFrom(startDate),
                createdTo(endDate));
    }
}
