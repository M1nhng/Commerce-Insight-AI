package com.commerceinsight.order.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.order.domain.*;
import com.commerceinsight.order.event.*;
import com.commerceinsight.order.repository.OrderRepository;
import com.commerceinsight.order.repository.OrderStatusHistoryRepository;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * OrderStatusTransitionService — enforces the order status state machine.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>ALL status changes MUST go through this service.</li>
 *   <li>Every valid transition creates an {@link OrderStatusHistory} record.</li>
 *   <li>Invalid transitions throw {@link BusinessRuleException} (HTTP 422).</li>
 *   <li>Inventory lifecycle hooks (ship → SALE, cancel → release) are triggered here.</li>
 * </ul>
 *
 * <p>State machine:
 * <pre>
 *   PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → COMPLETED
 *   PENDING / CONFIRMED / PROCESSING → CANCELLED
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusTransitionService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderInventoryService orderInventoryService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Transitions {@code order} to {@code targetStatus}, validates the transition,
     * records history, updates lifecycle timestamps, and triggers inventory hooks.
     *
     * @param order        the order to update (must be a managed JPA entity)
     * @param targetStatus the desired new status
     * @param changedBy    the user requesting the change
     * @param reason       optional reason for the transition
     */
    @Transactional
    public void transition(Order order, OrderStatus targetStatus, User changedBy, String reason) {
        OrderStatus current = order.getStatus();

        // Validate transition
        if (!current.allowedTransitions().contains(targetStatus)) {
            throw new BusinessRuleException(
                    ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot transition order '%s' from %s to %s"
                            .formatted(order.getOrderNumber(), current, targetStatus));
        }

        // Apply transition
        order.setStatus(targetStatus);
        applyTimestamps(order, targetStatus);

        // Save the updated order
        orderRepository.save(order);

        // Record history
        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(current)
                .toStatus(targetStatus)
                .changedBy(changedBy)
                .reason(reason)
                .build());

        // Trigger inventory lifecycle + domain events
        handleLifecycle(order, targetStatus, changedBy);

        log.info("Order {} transitioned: {} → {} by userId={}",
                order.getOrderNumber(), current, targetStatus,
                changedBy != null ? changedBy.getId() : "system");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void applyTimestamps(Order order, OrderStatus status) {
        Instant now = Instant.now();
        switch (status) {
            case SHIPPED   -> order.setShippedAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case CANCELLED -> order.setCancelledAt(now);
            case COMPLETED -> order.setCompletedAt(now);
            default        -> { /* no timestamp for CONFIRMED, PROCESSING */ }
        }
    }

    private void handleLifecycle(Order order, OrderStatus status, User changedBy) {
        switch (status) {
            case CONFIRMED  -> eventPublisher.publishEvent(
                    new OrderConfirmedEvent(order.getId(), order.getOrderNumber()));
            case SHIPPED -> {
                orderInventoryService.commitInventory(order, changedBy);
                eventPublisher.publishEvent(
                        new OrderShippedEvent(order.getId(), order.getOrderNumber()));
            }
            case DELIVERED  -> eventPublisher.publishEvent(
                    new OrderDeliveredEvent(order.getId(), order.getOrderNumber()));
            case COMPLETED  -> eventPublisher.publishEvent(
                    new OrderCompletedEvent(order.getId(), order.getOrderNumber()));
            case CANCELLED  -> {
                orderInventoryService.releaseInventory(order);
                eventPublisher.publishEvent(
                        new OrderCancelledEvent(order.getId(), order.getOrderNumber(), null));
            }
            default -> { /* no extra hooks for PROCESSING */ }
        }
    }
}
