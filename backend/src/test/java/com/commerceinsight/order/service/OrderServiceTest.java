package com.commerceinsight.order.service;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.order.domain.Order;
import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentMethod;
import com.commerceinsight.order.dto.request.CancelOrderRequest;
import com.commerceinsight.order.dto.request.CreateOrderItemRequest;
import com.commerceinsight.order.dto.request.CreateOrderRequest;
import com.commerceinsight.order.dto.request.UpdateOrderStatusRequest;
import com.commerceinsight.order.dto.response.OrderResponse;
import com.commerceinsight.order.mapper.OrderMapper;
import com.commerceinsight.order.repository.OrderRepository;
import com.commerceinsight.order.repository.OrderStatusHistoryRepository;
import com.commerceinsight.order.repository.PaymentRepository;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.repository.ProductRepository;
import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderService} guard / conflict paths (Sprint 15 —
 * closing the OrderService coverage gap). The full happy-path reservation flow
 * is exercised by {@code OrderControllerIntegrationTest}; here we lock down the
 * validation branches that map to 404 / 422.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — guards")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository historyRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerService customerService;
    @Mock private OrderInventoryService orderInventoryService;
    @Mock private OrderStatusTransitionService transitionService;
    @Mock private OrderCalculationService calculationService;
    @Mock private OrderMapper orderMapper;
    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private OrderService orderService;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private CreateOrderRequest createRequest() {
        return new CreateOrderRequest(customerId,
                List.of(new CreateOrderItemRequest(productId, 2, null)),
                null, null, PaymentMethod.CASH, null, null, null, null, null);
    }

    private Customer customer(CustomerStatus status) {
        Customer c = new Customer();
        c.setStatus(status);
        c.setCustomerCode("CUST-1");
        return c;
    }

    // ── findById / getOrThrow ────────────────────────────────────────────

    @Test
    @DisplayName("getOrThrow — unknown id → ResourceNotFoundException(ORDER_NOT_FOUND)")
    void getOrThrow_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrThrow(orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    // ── createOrder guards ───────────────────────────────────────────────

    @Test
    @DisplayName("createOrder — blocked customer → BusinessRuleException(CUSTOMER_BLOCKED), no reservation")
    void createOrder_blockedCustomer() {
        when(customerService.getOrThrow(customerId)).thenReturn(customer(CustomerStatus.BLOCKED));

        assertThatThrownBy(() -> orderService.createOrder(createRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CUSTOMER_BLOCKED);
        verify(orderInventoryService, never()).reserveInventory(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder — inactive customer → BusinessRuleException(CUSTOMER_INACTIVE)")
    void createOrder_inactiveCustomer() {
        when(customerService.getOrThrow(customerId)).thenReturn(customer(CustomerStatus.INACTIVE));

        assertThatThrownBy(() -> orderService.createOrder(createRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CUSTOMER_INACTIVE);
    }

    @Test
    @DisplayName("createOrder — product not found → ResourceNotFoundException(PRODUCT_NOT_FOUND)")
    void createOrder_productNotFound() {
        when(customerService.getOrThrow(customerId)).thenReturn(customer(CustomerStatus.ACTIVE));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(createRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
        verify(orderInventoryService, never()).reserveInventory(any());
    }

    @Test
    @DisplayName("createOrder — inactive product → BusinessRuleException(PRODUCT_INACTIVE)")
    void createOrder_inactiveProduct() {
        when(customerService.getOrThrow(customerId)).thenReturn(customer(CustomerStatus.ACTIVE));
        Product p = new Product();
        p.setActive(false);
        p.setSku("SKU-X");
        when(productRepository.findById(productId)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> orderService.createOrder(createRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PRODUCT_INACTIVE);
    }

    // ── cancelOrder ─────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder — SHIPPED order → BusinessRuleException(ORDER_CANNOT_CANCEL), no transition")
    void cancelOrder_notCancellable() {
        Order order = new Order();
        order.setStatus(OrderStatus.SHIPPED);
        order.setOrderNumber("ORD-1");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, new CancelOrderRequest("changed mind")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ORDER_CANNOT_CANCEL);
        verify(transitionService, never()).transition(any(), any(), any(), any());
    }

    @Test
    @DisplayName("cancelOrder — PENDING order → transitions to CANCELLED")
    void cancelOrder_ok() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        order.setOrderNumber("ORD-2");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(securityContextHelper.getCurrentUserOrThrow()).thenReturn(new User());
        lenient().when(orderMapper.toResponse(any())).thenReturn(mockResponse());

        orderService.cancelOrder(orderId, new CancelOrderRequest("out of stock"));

        verify(transitionService).transition(eq(order), eq(OrderStatus.CANCELLED), any(), eq("out of stock"));
    }

    // ── updateStatus ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus — unknown order → 404")
    void updateStatus_notFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.updateStatus(orderId,
                new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateStatus — delegates the target status to OrderStatusTransitionService")
    void updateStatus_delegates() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(securityContextHelper.getCurrentUserOrThrow()).thenReturn(new User());
        lenient().when(orderMapper.toResponse(any())).thenReturn(mockResponse());

        orderService.updateStatus(orderId, new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "ok"));

        verify(transitionService).transition(eq(order), eq(OrderStatus.CONFIRMED), any(), eq("ok"));
    }

    private static OrderResponse mockResponse() {
        return org.mockito.Mockito.mock(OrderResponse.class);
    }
}
