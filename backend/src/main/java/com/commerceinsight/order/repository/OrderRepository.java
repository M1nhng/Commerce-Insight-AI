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
     * Find order by ID, eagerly loading items, addresses, statusHistory and payment
     * to avoid N+1 when building the full OrderResponse.
     */
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH o.addresses a
            LEFT JOIN FETCH o.statusHistory sh
            LEFT JOIN FETCH o.payment p
            LEFT JOIN FETCH o.customer c
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithDetails(@Param("id") UUID id);

    /** Count orders for a given customer. */
    long countByCustomerId(UUID customerId);
}
