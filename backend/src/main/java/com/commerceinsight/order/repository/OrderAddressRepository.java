package com.commerceinsight.order.repository;

import com.commerceinsight.order.domain.OrderAddress;
import com.commerceinsight.order.domain.OrderAddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** OrderAddressRepository — data access for immutable {@link OrderAddress} snapshots. */
@Repository
public interface OrderAddressRepository extends JpaRepository<OrderAddress, UUID> {
    List<OrderAddress> findAllByOrderId(UUID orderId);
    Optional<OrderAddress> findByOrderIdAndType(UUID orderId, OrderAddressType type);
}
