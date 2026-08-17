package com.commerceinsight.order.service;

import com.commerceinsight.inventory.service.InventoryService;
import com.commerceinsight.order.domain.Order;
import com.commerceinsight.order.domain.OrderItem;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * OrderInventoryService — bridges the Order domain with the Inventory domain.
 *
 * <p>Architecture rules:
 * <ul>
 *   <li>NEVER inject InventoryRepository directly — always go through InventoryService.</li>
 *   <li>No business logic here — just orchestrates calls to InventoryService per line item.</li>
 *   <li>Each method is idempotent-safe: InventoryService handles lock + guard.</li>
 * </ul>
 *
 * <p>Inventory Lifecycle:
 * <pre>
 *   Order CREATED   → reserveInventory()   (reservedQty += itemQty, no physical deduction)
 *   Order SHIPPED   → commitInventory()    (qty -= itemQty, reservedQty -= itemQty + SALE txn)
 *   Order CANCELLED → releaseInventory()   (reservedQty -= itemQty, no physical deduction)
 * </pre>
 *
 * <p>Double-decrement prevention:
 * commitInventory is only called on SHIPPED (once). releaseInventory is only called on
 * CANCELLED (once). Both are guarded by the state machine in OrderStatusTransitionService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderInventoryService {

    private final InventoryService inventoryService;

    /**
     * Reserves inventory for all items in the order.
     * Called at order creation. Uses pessimistic lock inside InventoryService.
     *
     * @param order  the newly created order
     * @throws com.commerceinsight.exception.BusinessRuleException if any item has insufficient stock
     */
    @Transactional
    public void reserveInventory(Order order) {
        UUID orderId = order.getId();
        for (OrderItem item : order.getItems()) {
            UUID productId = item.getProduct() != null ? item.getProduct().getId() : null;
            if (productId == null) {
                log.warn("OrderItem has no product reference — skipping reservation. orderId={}", orderId);
                continue;
            }
            inventoryService.reserveStock(productId, item.getQuantity(), orderId);
        }
        log.info("Inventory reserved for all items. orderId={}, itemCount={}", orderId, order.getItems().size());
    }

    /**
     * Releases inventory reservation when an order is cancelled.
     * Does NOT physically deduct stock — only reduces reservedQuantity.
     *
     * @param order the cancelled order
     */
    @Transactional
    public void releaseInventory(Order order) {
        UUID orderId = order.getId();
        for (OrderItem item : order.getItems()) {
            UUID productId = item.getProduct() != null ? item.getProduct().getId() : null;
            if (productId == null) continue;
            inventoryService.releaseReservation(productId, item.getQuantity(), orderId);
        }
        log.info("Inventory reservation released. orderId={}", orderId);
    }

    /**
     * Commits physical stock deduction when the order ships.
     * Decrements quantity AND reservedQuantity, records a SALE transaction.
     *
     * @param order     the order being shipped
     * @param shippedBy the user who triggered the SHIPPED transition
     */
    @Transactional
    public void commitInventory(Order order, User shippedBy) {
        UUID orderId = order.getId();
        for (OrderItem item : order.getItems()) {
            UUID productId = item.getProduct() != null ? item.getProduct().getId() : null;
            if (productId == null) continue;
            inventoryService.commitSale(productId, item.getQuantity(), orderId, shippedBy);
        }
        log.info("Inventory sale committed for all items. orderId={}", orderId);
    }
}
