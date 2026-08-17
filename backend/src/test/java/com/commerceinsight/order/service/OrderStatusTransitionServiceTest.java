package com.commerceinsight.order.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.order.domain.*;
import com.commerceinsight.order.event.*;
import com.commerceinsight.order.repository.OrderRepository;
import com.commerceinsight.order.repository.OrderStatusHistoryRepository;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderStatusTransitionService — tests every valid and invalid transition.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusTransitionService")
class OrderStatusTransitionServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private OrderInventoryService orderInventoryService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private OrderStatusTransitionService transitionService;

    @BeforeEach
    void setUp() {
        transitionService = new OrderStatusTransitionService(
                orderRepository, historyRepository, orderInventoryService, eventPublisher);
    }

    private Order orderWithStatus(OrderStatus status) {
        Order order = new Order();
        order.setStatus(status);
        order.setOrderNumber("ORD-202608-001234");
        order.setItems(new ArrayList<>());
        order.setAddresses(new ArrayList<>());
        order.setStatusHistory(new ArrayList<>());
        return order;
    }

    private User mockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Admin");
        user.setLastName("Test");
        return user;
    }

    // ── Valid transitions ────────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING → CONFIRMED succeeds")
    void transition_pendingToConfirmed_succeeds() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        transitionService.transition(order, OrderStatus.CONFIRMED, mockUser(), "approved");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
        verify(historyRepository).save(any(OrderStatusHistory.class));
        verify(eventPublisher).publishEvent(any(OrderConfirmedEvent.class));
    }

    @Test
    @DisplayName("CONFIRMED → PROCESSING succeeds")
    void transition_confirmedToProcessing_succeeds() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        transitionService.transition(order, OrderStatus.PROCESSING, mockUser(), null);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    @DisplayName("PROCESSING → SHIPPED succeeds and triggers commitInventory")
    void transition_processingToShipped_commitsInventory() {
        Order order = orderWithStatus(OrderStatus.PROCESSING);
        User user = mockUser();
        transitionService.transition(order, OrderStatus.SHIPPED, user, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getShippedAt()).isNotNull();
        verify(orderInventoryService).commitInventory(order, user);
        verify(eventPublisher).publishEvent(any(OrderShippedEvent.class));
    }

    @Test
    @DisplayName("SHIPPED → DELIVERED succeeds")
    void transition_shippedToDelivered_succeeds() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        transitionService.transition(order, OrderStatus.DELIVERED, mockUser(), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(OrderDeliveredEvent.class));
    }

    @Test
    @DisplayName("DELIVERED → COMPLETED succeeds")
    void transition_deliveredToCompleted_succeeds() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        transitionService.transition(order, OrderStatus.COMPLETED, mockUser(), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName("PENDING → CANCELLED succeeds and releases inventory")
    void transition_pendingToCancelled_releasesInventory() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        transitionService.transition(order, OrderStatus.CANCELLED, mockUser(), "customer request");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
        verify(orderInventoryService).releaseInventory(order);
        verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
    }

    @Test
    @DisplayName("CONFIRMED → CANCELLED succeeds and releases inventory")
    void transition_confirmedToCancelled_releasesInventory() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        transitionService.transition(order, OrderStatus.CANCELLED, mockUser(), null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderInventoryService).releaseInventory(order);
    }

    // ── Invalid transitions ──────────────────────────────────────────────────

    @Test
    @DisplayName("PENDING → SHIPPED throws INVALID_STATUS_TRANSITION")
    void transition_pendingToShipped_throws() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        assertThatThrownBy(() ->
                transitionService.transition(order, OrderStatus.SHIPPED, mockUser(), null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("SHIPPED → CANCELLED throws INVALID_STATUS_TRANSITION")
    void transition_shippedToCancelled_throws() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        assertThatThrownBy(() ->
                transitionService.transition(order, OrderStatus.CANCELLED, mockUser(), null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("COMPLETED → any status throws INVALID_STATUS_TRANSITION")
    void transition_completed_isTerminal() {
        Order order = orderWithStatus(OrderStatus.COMPLETED);
        for (OrderStatus target : OrderStatus.values()) {
            if (target == OrderStatus.COMPLETED) continue;
            assertThatThrownBy(() ->
                    transitionService.transition(order, target, mockUser(), null))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    @DisplayName("CANCELLED → any status throws INVALID_STATUS_TRANSITION")
    void transition_cancelled_isTerminal() {
        Order order = orderWithStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() ->
                transitionService.transition(order, OrderStatus.PENDING, mockUser(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("DELIVERED → CANCELLED throws INVALID_STATUS_TRANSITION")
    void transition_deliveredToCancelled_throws() {
        Order order = orderWithStatus(OrderStatus.DELIVERED);
        assertThatThrownBy(() ->
                transitionService.transition(order, OrderStatus.CANCELLED, mockUser(), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── History record ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Status history record captures from/to/user/reason")
    void transition_recordsHistoryWithCorrectFields() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        User user = mockUser();
        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);

        transitionService.transition(order, OrderStatus.CONFIRMED, user, "test reason");

        verify(historyRepository).save(captor.capture());
        OrderStatusHistory history = captor.getValue();
        assertThat(history.getFromStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.getToStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.getChangedBy()).isEqualTo(user);
        assertThat(history.getReason()).isEqualTo("test reason");
    }
}
