package com.commerceinsight.order.repository;

import com.commerceinsight.order.domain.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** OrderStatusHistoryRepository — append-only audit trail access. */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
