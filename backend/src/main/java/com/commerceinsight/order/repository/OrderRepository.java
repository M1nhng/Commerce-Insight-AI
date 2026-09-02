package com.commerceinsight.order.repository;

import com.commerceinsight.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * OrderRepository — data access for {@link Order} entities.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>,
        JpaSpecificationExecutor<Order> {

    /** Check if an order number already exists (for uniqueness validation). */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find order by ID, eagerly loading items, payment and customer.
     *
     * <p>Only ONE collection ({@code items}) is fetch-joined here: {@code items},
     * {@code addresses} and {@code statusHistory} are all {@code List}-typed
     * {@code @OneToMany} bags, and fetch-joining more than one bag in a single
     * query throws Hibernate's {@code MultipleBagFetchException} (previously
     * surfaced as an HTTP 500 on every {@code GET /orders/{id}} — Sprint 14).
     * {@code addresses} and {@code statusHistory} initialise lazily while the
     * caller's read-only transaction is still open (see
     * {@code OrderService#findById}), so the full {@link OrderResponse} is
     * unchanged — at the cost of two extra selects for a single-order view.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH o.payment p
            LEFT JOIN FETCH o.customer c
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    /** Count orders for a given customer. */
    long countByCustomerId(UUID customerId);
}
