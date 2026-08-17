package com.commerceinsight.order.service;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.order.domain.*;
import com.commerceinsight.order.dto.request.*;
import com.commerceinsight.order.dto.response.*;
import com.commerceinsight.order.event.OrderCreatedEvent;
import com.commerceinsight.order.mapper.OrderMapper;
import com.commerceinsight.order.repository.*;
import com.commerceinsight.order.specification.OrderSpecification;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.repository.ProductRepository;
import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OrderService — orchestrates the full order lifecycle.
 *
 * <p>Business Invariants:
 * <ol>
 *   <li>Customer must exist and be ACTIVE before order creation.</li>
 *   <li>All products must exist and be active.</li>
 *   <li>Quantities must be positive (enforced by DTO validation).</li>
 *   <li>Inventory must be sufficient (enforced via pessimistic lock in InventoryService).</li>
 *   <li>Backend always recalculates totals — never trusts client values.</li>
 *   <li>All status transitions go through OrderStatusTransitionService.</li>
 *   <li>Inventory is reserved on creation, released on cancel, committed on ship.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository            paymentRepository;
    private final ProductRepository            productRepository;
    private final CustomerService              customerService;
    private final OrderInventoryService        orderInventoryService;
    private final OrderStatusTransitionService transitionService;
    private final OrderCalculationService      calculationService;
    private final OrderMapper                  orderMapper;
    private final SecurityContextHelper        securityContextHelper;
    private final ApplicationEventPublisher    eventPublisher;

    private static final DateTimeFormatter ORDER_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> findAll(
            String keyword, UUID customerId, OrderStatus status,
            PaymentStatus paymentStatus, Instant dateFrom, Instant dateTo,
            Pageable pageable) {

        Specification<Order> spec = OrderSpecification.build(
                keyword, customerId, status, paymentStatus, dateFrom, dateTo);
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(orderMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND,
                        "Order with ID '%s' was not found".formatted(id)));
        return orderMapper.toResponse(order);
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Creates a new order following the 13-step creation protocol:
     * <ol>
     *   <li>Validate customer exists and is ACTIVE</li>
     *   <li>Validate all products exist and are active</li>
     *   <li>Validate quantities are positive (DTO)</li>
     *   <li>Validate inventory exists (inside reserveStock)</li>
     *   <li>Validate sufficient available inventory (inside reserveStock)</li>
     *   <li>Reserve inventory (pessimistic lock)</li>
     *   <li>Create order entity</li>
     *   <li>Create order items with product snapshots</li>
     *   <li>Create address snapshots</li>
     *   <li>Calculate monetary totals</li>
     *   <li>Create payment record</li>
     *   <li>Create initial status history</li>
     *   <li>Publish OrderCreatedEvent</li>
     * </ol>
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Step 1: Validate customer
        Customer customer = customerService.getOrThrow(request.customerId());
        if (customer.getStatus() == CustomerStatus.BLOCKED) {
            throw new BusinessRuleException(ErrorCode.CUSTOMER_BLOCKED,
                    "Customer '%s' is blocked and cannot place orders".formatted(customer.getCustomerCode()));
        }
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new BusinessRuleException(ErrorCode.CUSTOMER_INACTIVE,
                    "Customer '%s' is not active".formatted(customer.getCustomerCode()));
        }

        // Step 2: Validate products
        List<Product> products = resolveProducts(request.items());

        // Step 7–8: Build Order + Items (inventory reservation happens before save)
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .currency(StringUtils.hasText(request.currency()) ? request.currency() : "VND")
                .notes(request.notes())
                .shippingFee(nullSafe(request.shippingFee()))
                .discount(nullSafe(request.discount()))
                .tax(nullSafe(request.tax()))
                .build();

        List<OrderItem> items = buildItems(order, request.items(), products);
        order.setItems(items);

        // Step 10: Calculate totals (backend always recalculates)
        BigDecimal subtotal = calculationService.calculateSubtotal(items);
        order.setSubtotal(subtotal);
        BigDecimal total = calculationService.calculateTotal(
                subtotal, order.getDiscount(), order.getShippingFee(), order.getTax());
        order.setTotal(total);

        // Step 9: Address snapshots
        if (request.shippingAddress() != null) {
            order.getAddresses().add(buildAddress(order, request.shippingAddress()));
        }
        if (request.billingAddress() != null) {
            order.getAddresses().add(buildAddress(order, request.billingAddress()));
        }

        // Save order (generates ID needed for relations)
        Order saved = orderRepository.save(order);

        // Steps 5–6: Reserve inventory (after order is persisted — orderId needed for txn record)
        orderInventoryService.reserveInventory(saved);

        // Step 11: Create payment record
        Payment payment = Payment.builder()
                .order(saved)
                .method(request.paymentMethod())
                .amount(total)
                .currency(saved.getCurrency())
                .build();
        paymentRepository.save(payment);

        // Step 12: Initial status history (null fromStatus = creation entry)
        User currentUser = securityContextHelper.getCurrentUserOrThrow();
        historyRepository.save(OrderStatusHistory.builder()
                .order(saved)
                .fromStatus(null)
                .toStatus(OrderStatus.PENDING)
                .changedBy(currentUser)
                .reason("Order created")
                .build());

        // Step 13: Publish domain event
        eventPublisher.publishEvent(new OrderCreatedEvent(
                saved.getId(), saved.getOrderNumber(),
                customer.getId(), items.size()));

        log.info("Order created: orderNumber={}, customerId={}, items={}, total={}",
                saved.getOrderNumber(), customer.getId(), items.size(), total);

        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(saved.getId()).orElseThrow());
    }

    /**
     * Updates order status via the state machine.
     */
    @Transactional
    public OrderResponse updateStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = getOrThrow(orderId);
        User currentUser = securityContextHelper.getCurrentUserOrThrow();
        transitionService.transition(order, request.status(), currentUser, request.reason());
        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    /**
     * Cancels an order. Only valid from PENDING, CONFIRMED, or PROCESSING.
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request) {
        Order order = getOrThrow(orderId);

        if (!order.getStatus().isCancellable()) {
            throw new BusinessRuleException(
                    ErrorCode.ORDER_CANNOT_CANCEL,
                    "Order '%s' cannot be cancelled from status %s"
                            .formatted(order.getOrderNumber(), order.getStatus()));
        }

        User currentUser = securityContextHelper.getCurrentUserOrThrow();
        transitionService.transition(order, OrderStatus.CANCELLED, currentUser,
                request != null ? request.reason() : null);

        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    public Order getOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ORDER_NOT_FOUND,
                        "Order with ID '%s' was not found".formatted(id)));
    }

    private List<Product> resolveProducts(List<CreateOrderItemRequest> itemRequests) {
        List<Product> products = new ArrayList<>();
        for (CreateOrderItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ErrorCode.PRODUCT_NOT_FOUND,
                            "Product with ID '%s' was not found".formatted(req.productId())));
            if (!product.isActive()) {
                throw new BusinessRuleException(ErrorCode.PRODUCT_INACTIVE,
                        "Product '%s' is not active and cannot be ordered".formatted(product.getSku()));
            }
            products.add(product);
        }
        return products;
    }

    private List<OrderItem> buildItems(Order order,
                                       List<CreateOrderItemRequest> requests,
                                       List<Product> products) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            CreateOrderItemRequest req = requests.get(i);
            Product product = products.get(i);
            BigDecimal discountAmount = nullSafe(req.discountAmount());

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .skuSnapshot(product.getSku())
                    .productNameSnapshot(product.getName())
                    .productSku(product.getSku())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(req.quantity())
                    .discountAmount(discountAmount)
                    .discount(discountAmount)
                    .build();
            item.calculateSubtotal();
            items.add(item);
        }
        return items;
    }

    private OrderAddress buildAddress(Order order, CreateOrderAddressRequest req) {
        return OrderAddress.builder()
                .order(order)
                .type(req.type())
                .recipientName(req.recipientName())
                .phone(req.phone())
                .addressLine(req.addressLine())
                .ward(req.ward())
                .district(req.district())
                .province(req.province())
                .country(StringUtils.hasText(req.country()) ? req.country() : "Vietnam")
                .build();
    }

    private String generateOrderNumber() {
        String datePart = YearMonth.now().format(ORDER_DATE_FMT);
        String seq = String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999_999));
        String candidate = "ORD-" + datePart + "-" + seq;
        // Retry if collision (extremely rare)
        while (orderRepository.existsByOrderNumber(candidate)) {
            seq = String.format("%06d", ThreadLocalRandom.current().nextInt(1, 999_999));
            candidate = "ORD-" + datePart + "-" + seq;
        }
        return candidate;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
