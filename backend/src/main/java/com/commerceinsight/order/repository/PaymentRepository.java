package com.commerceinsight.order.repository;

import com.commerceinsight.order.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** PaymentRepository — data access for simulated {@link Payment} records. */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
}
