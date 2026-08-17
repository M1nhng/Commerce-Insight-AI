package com.commerceinsight.inventory.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.inventory.domain.*;
import com.commerceinsight.inventory.dto.request.AdjustStockRequest;
import com.commerceinsight.inventory.dto.request.TransferStockRequest;
import com.commerceinsight.inventory.dto.response.InventoryResponse;
import com.commerceinsight.inventory.dto.response.InventoryTransactionResponse;
import com.commerceinsight.inventory.mapper.InventoryMapper;
import com.commerceinsight.inventory.mapper.InventoryTransactionMapper;
import com.commerceinsight.inventory.repository.InventoryRepository;
import com.commerceinsight.inventory.repository.InventoryTransactionRepository;
import com.commerceinsight.inventory.specification.InventorySpecification;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.repository.ProductRepository;
import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * InventoryService — business logic for inventory management.
 *
 * <p>Business Invariants enforced here:
 * <ol>
 *   <li>Quantity NEVER goes below 0.</li>
 *   <li>EVERY quantity change creates an InventoryTransaction via
 *       {@link InventoryTransactionService}.</li>
 *   <li>Transfers are atomic: TRANSFER_OUT + TRANSFER_IN in one transaction.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseService warehouseService;
    private final ProductRepository productRepository;
    private final InventoryTransactionService transactionService;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final SecurityContextHelper securityContextHelper;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> findAll(
            UUID warehouseId, UUID productId, String search,
            Boolean lowStockOnly, Pageable pageable) {

        Specification<Inventory> spec = InventorySpecification.build(
                warehouseId, productId, search, lowStockOnly);
        Page<Inventory> page = inventoryRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(inventoryMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public InventoryResponse findById(UUID id) {
        return inventoryMapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> findByProduct(UUID productId) {
        return inventoryRepository.findAllByProductId(productId)
                .stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> findLowStock() {
        return inventoryRepository.findLowStockItems()
                .stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> getTransactions(UUID inventoryId, Pageable pageable) {
        getOrThrow(inventoryId); // validate inventory exists
        Page<InventoryTransaction> page = transactionRepository
                .findAllByInventoryIdOrderByCreatedAtDesc(inventoryId, pageable);
        return PageResponse.from(page.map(transactionMapper::toResponse));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Creates an initial inventory record for a product in a warehouse.
     * Called internally when a product is created with {@code initialStock > 0}.
     */
    @Transactional
    public Inventory initializeInventory(Product product, Warehouse warehouse, int initialQuantity) {
        Inventory inventory = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        Inventory saved = inventoryRepository.save(inventory);

        if (initialQuantity > 0) {
            applyQuantityChange(saved, initialQuantity, TransactionType.PURCHASE,
                    null, null, "Initial stock on product creation");
        }
        return saved;
    }

    /**
     * Immediate, admin-level stock adjustment — no approval required.
     * Creates an ADJUSTMENT transaction record.
     */
    @Transactional
    public InventoryResponse adjustStock(UUID inventoryId, AdjustStockRequest request) {
        Inventory inventory = getOrThrow(inventoryId);
        User currentUser = securityContextHelper.getCurrentUserOrThrow();

        int qtyDelta = request.quantity();
        int qtyBefore = inventory.getQuantity();
        int qtyAfter = qtyBefore + qtyDelta;

        // INVARIANT: never allow negative stock
        if (qtyAfter < 0) {
            throw new BusinessRuleException(
                    ErrorCode.NEGATIVE_STOCK_NOT_ALLOWED,
                    "Adjustment would result in negative stock. Current: %d, Delta: %d"
                            .formatted(qtyBefore, qtyDelta));
        }

        // Apply the change
        inventory.setQuantity(qtyAfter);
        if (request.lowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.lowStockThreshold());
        }
        inventoryRepository.save(inventory);

        transactionService.record(inventory, TransactionType.ADJUSTMENT,
                qtyDelta, qtyBefore, qtyAfter, currentUser, null, request.notes());

        log.info("Stock adjusted: inventoryId={}, delta={}, by={}", inventoryId, qtyDelta, currentUser.getId());
        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Atomically transfers stock from one warehouse to another.
     * Creates TRANSFER_OUT + TRANSFER_IN transaction records.
     * Destination inventory row is created on the fly if needed.
     */
    @Transactional
    public void transfer(TransferStockRequest request) {
        // Validate same-warehouse guard
        if (request.sourceWarehouseId().equals(request.destinationWarehouseId())) {
            throw new BusinessRuleException(
                    ErrorCode.SAME_WAREHOUSE_TRANSFER,
                    "Source and destination warehouses must be different");
        }

        User currentUser = securityContextHelper.getCurrentUserOrThrow();

        // Resolve warehouses
        Warehouse sourceWarehouse = warehouseService.getOrThrow(request.sourceWarehouseId());
        Warehouse destWarehouse   = warehouseService.getOrThrow(request.destinationWarehouseId());

        // Resolve product
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Product '%s' not found".formatted(request.productId())));

        // Get source inventory
        Inventory source = inventoryRepository
                .findByProductIdAndWarehouseId(request.productId(), request.sourceWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVENTORY_NOT_FOUND,
                        "No inventory found for product '%s' in warehouse '%s'"
                                .formatted(product.getName(), sourceWarehouse.getName())));

        // INVARIANT: enough stock to transfer
        if (source.getAvailableQuantity() < request.quantity()) {
            throw new BusinessRuleException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient available stock. Available: %d, Requested: %d"
                            .formatted(source.getAvailableQuantity(), request.quantity()));
        }

        // Get or create destination inventory
        Inventory dest = inventoryRepository
                .findByProductIdAndWarehouseId(request.productId(), request.destinationWarehouseId())
                .orElseGet(() -> inventoryRepository.save(
                        Inventory.builder()
                                .product(product)
                                .warehouse(destWarehouse)
                                .quantity(0)
                                .reservedQuantity(0)
                                .build()));

        String transferNote = request.notes() != null ? request.notes()
                : "Transfer: %s → %s".formatted(sourceWarehouse.getCode(), destWarehouse.getCode());

        // Deduct from source
        int srcBefore = source.getQuantity();
        source.setQuantity(srcBefore - request.quantity());
        inventoryRepository.save(source);
        InventoryTransaction outTxn = transactionService.record(
                source, TransactionType.TRANSFER_OUT,
                -request.quantity(), srcBefore, source.getQuantity(),
                currentUser, null, transferNote);

        // Add to destination
        int dstBefore = dest.getQuantity();
        dest.setQuantity(dstBefore + request.quantity());
        inventoryRepository.save(dest);
        transactionService.record(
                dest, TransactionType.TRANSFER_IN,
                request.quantity(), dstBefore, dest.getQuantity(),
                currentUser, outTxn.getId(), transferNote);

        log.info("Stock transferred: product={}, qty={}, from={}, to={}",
                request.productId(), request.quantity(),
                request.sourceWarehouseId(), request.destinationWarehouseId());
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Helper used by initializeInventory and other internal callers that already
     * have the inventory entity and need to record a quantity delta with a transaction.
     */
    private void applyQuantityChange(Inventory inventory, int delta, TransactionType type,
                                     User user, UUID referenceId, String notes) {
        int before = inventory.getQuantity();
        int after = before + delta;
        inventory.setQuantity(after);
        inventoryRepository.save(inventory);
        transactionService.record(inventory, type, delta, before, after, user, referenceId, notes);
    }

    public Inventory getOrThrow(UUID id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.INVENTORY_NOT_FOUND,
                        "Inventory record with ID '%s' was not found".formatted(id)));
    }

    // ── Order integration (called by OrderInventoryService) ───────────────────

    /**
     * Reserves {@code quantity} units of the given product for an order.
     * Uses a PESSIMISTIC WRITE lock to prevent concurrent overselling.
     *
     * <p>Strategy: pick the warehouse with the most available stock (greedy).
     *
     * @throws BusinessRuleException (INSUFFICIENT_STOCK) if no warehouse has enough available stock
     */
    @Transactional
    public void reserveStock(UUID productId, int quantity, UUID orderId) {
        List<Inventory> rows = inventoryRepository.findAllByProductIdWithLock(productId);
        if (rows.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "No inventory found for product '%s'".formatted(productId));
        }

        // Greedy: pick the warehouse with the most available stock
        Inventory best = rows.stream()
                .max(java.util.Comparator.comparingInt(Inventory::getAvailableQuantity))
                .orElseThrow();

        if (best.getAvailableQuantity() < quantity) {
            throw new BusinessRuleException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Insufficient stock for product '%s'. Available: %d, Requested: %d"
                            .formatted(productId, best.getAvailableQuantity(), quantity));
        }

        best.setReservedQuantity(best.getReservedQuantity() + quantity);
        inventoryRepository.save(best);
        log.info("Inventory reserved: productId={}, qty={}, orderId={}", productId, quantity, orderId);
    }

    /**
     * Releases a reservation previously made by {@link #reserveStock}.
     * Called on order cancellation. Does NOT decrement physical stock.
     *
     * @throws BusinessRuleException (INSUFFICIENT_STOCK) if no inventory found for this product
     */
    @Transactional
    public void releaseReservation(UUID productId, int quantity, UUID orderId) {
        List<Inventory> rows = inventoryRepository.findAllByProductIdWithLock(productId);
        if (rows.isEmpty()) {
            log.warn("Cannot release reservation — no inventory found for product '{}'. orderId={}",
                    productId, orderId);
            return;
        }

        // Find the row that has enough reserved quantity to release from
        Inventory target = rows.stream()
                .filter(i -> i.getReservedQuantity() >= quantity)
                .findFirst()
                .orElse(rows.get(0)); // fallback — release whatever we can

        int newReserved = Math.max(0, target.getReservedQuantity() - quantity);
        target.setReservedQuantity(newReserved);
        inventoryRepository.save(target);
        log.info("Inventory reservation released: productId={}, qty={}, orderId={}", productId, quantity, orderId);
    }

    /**
     * Commits a physical stock deduction when an order is shipped (SALE).
     * Decrements both {@code quantity} and {@code reservedQuantity}.
     * Records a {@code SALE} InventoryTransaction for the audit trail.
     *
     * @throws BusinessRuleException (INSUFFICIENT_STOCK) if physical stock would go negative
     */
    @Transactional
    public void commitSale(UUID productId, int quantity, UUID orderId, User user) {
        List<Inventory> rows = inventoryRepository.findAllByProductIdWithLock(productId);
        if (rows.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "No inventory to commit for product '%s'".formatted(productId));
        }

        // Find row with enough reserved stock
        Inventory target = rows.stream()
                .filter(i -> i.getReservedQuantity() >= quantity)
                .findFirst()
                .orElseGet(() -> rows.stream()
                        .max(java.util.Comparator.comparingInt(Inventory::getReservedQuantity))
                        .orElse(rows.get(0)));

        int qtyBefore = target.getQuantity();
        int qtyAfter = qtyBefore - quantity;

        if (qtyAfter < 0) {
            throw new BusinessRuleException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Cannot commit sale: physical stock would go negative for product '%s'".formatted(productId));
        }

        int newReserved = Math.max(0, target.getReservedQuantity() - quantity);
        target.setQuantity(qtyAfter);
        target.setReservedQuantity(newReserved);
        inventoryRepository.save(target);

        transactionService.record(target, TransactionType.SALE,
                -quantity, qtyBefore, qtyAfter, user, orderId,
                "Sale committed for order " + orderId);

        log.info("Inventory sale committed: productId={}, qty={}, orderId={}", productId, quantity, orderId);
    }
}

