package com.commerceinsight.inventory.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.inventory.domain.*;
import com.commerceinsight.inventory.dto.request.RequestStockAdjustmentRequest;
import com.commerceinsight.inventory.dto.request.ReviewStockAdjustmentRequest;
import com.commerceinsight.inventory.dto.response.StockAdjustmentResponse;
import com.commerceinsight.inventory.mapper.StockAdjustmentMapper;
import com.commerceinsight.inventory.repository.StockAdjustmentRepository;
import com.commerceinsight.inventory.specification.StockAdjustmentSpecification;
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

import java.time.Instant;
import java.util.UUID;

/**
 * StockAdjustmentService — manages the approval workflow for stock corrections.
 *
 * <p>Lifecycle: PENDING → APPROVED (stock applied) | REJECTED (no stock change)
 *
 * <p>Business Rules:
 * <ul>
 *   <li>Only PENDING adjustments can be approved or rejected.</li>
 *   <li>When APPROVED, creates an InventoryTransaction of type ADJUSTMENT.</li>
 *   <li>The negative-stock invariant is checked at approval time.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final InventoryService inventoryService;
    private final InventoryTransactionService transactionService;
    private final StockAdjustmentMapper adjustmentMapper;
    private final SecurityContextHelper securityContextHelper;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentResponse> findAll(
            AdjustmentStatus status, UUID warehouseId, UUID productId,
            UUID requestedBy, Pageable pageable) {

        Specification<StockAdjustment> spec = StockAdjustmentSpecification.build(
                status, warehouseId, productId, requestedBy);
        Page<StockAdjustment> page = adjustmentRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(adjustmentMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public StockAdjustmentResponse findById(UUID id) {
        return adjustmentMapper.toResponse(getOrThrow(id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Submits a new stock adjustment request. Creates a PENDING adjustment record.
     * Does NOT modify inventory yet — awaits approval.
     */
    @Transactional
    public StockAdjustmentResponse request(RequestStockAdjustmentRequest req) {
        User currentUser = securityContextHelper.getCurrentUserOrThrow();
        Inventory inventory = inventoryService.getOrThrow(req.inventoryId());

        StockAdjustment adjustment = StockAdjustment.builder()
                .inventory(inventory)
                .product(inventory.getProduct())
                .warehouse(inventory.getWarehouse())
                .quantityDelta(req.quantityDelta())
                .reason(req.reason())
                .status(AdjustmentStatus.PENDING)
                .requestedBy(currentUser)
                .build();

        StockAdjustment saved = adjustmentRepository.save(adjustment);
        log.info("Stock adjustment requested: id={}, inventoryId={}, delta={}, by={}",
                saved.getId(), req.inventoryId(), req.quantityDelta(), currentUser.getId());
        return adjustmentMapper.toResponse(saved);
    }

    /**
     * Approves a PENDING adjustment and immediately applies the stock change.
     * Creates an InventoryTransaction of type ADJUSTMENT.
     */
    @Transactional
    public StockAdjustmentResponse approve(UUID adjustmentId, ReviewStockAdjustmentRequest req) {
        StockAdjustment adjustment = getPendingOrThrow(adjustmentId);
        User reviewer = securityContextHelper.getCurrentUserOrThrow();
        Inventory inventory = adjustment.getInventory();

        // INVARIANT: check no-negative-stock at approval time
        int newQty = inventory.getQuantity() + adjustment.getQuantityDelta();
        if (newQty < 0) {
            throw new BusinessRuleException(
                    ErrorCode.NEGATIVE_STOCK_NOT_ALLOWED,
                    "Approving this adjustment would result in negative stock. Current: %d, Delta: %d"
                            .formatted(inventory.getQuantity(), adjustment.getQuantityDelta()));
        }

        // Apply the stock change
        int qtyBefore = inventory.getQuantity();
        inventory.setQuantity(newQty);

        InventoryTransaction txn = transactionService.record(
                inventory, TransactionType.ADJUSTMENT,
                adjustment.getQuantityDelta(),
                qtyBefore, newQty,
                reviewer, adjustment.getId(),
                "Stock adjustment approved: " + adjustment.getReason());

        // Update adjustment status
        adjustment.setStatus(AdjustmentStatus.APPROVED);
        adjustment.setReviewedBy(reviewer);
        adjustment.setReviewedAt(Instant.now());
        adjustment.setReviewNotes(req == null ? null : req.reviewNotes());
        adjustment.setTransactionId(txn.getId());

        StockAdjustment saved = adjustmentRepository.save(adjustment);
        log.info("Stock adjustment approved: id={}, by={}", adjustmentId, reviewer.getId());
        return adjustmentMapper.toResponse(saved);
    }

    /**
     * Rejects a PENDING adjustment. No inventory change is made.
     */
    @Transactional
    public StockAdjustmentResponse reject(UUID adjustmentId, ReviewStockAdjustmentRequest req) {
        StockAdjustment adjustment = getPendingOrThrow(adjustmentId);
        User reviewer = securityContextHelper.getCurrentUserOrThrow();

        adjustment.setStatus(AdjustmentStatus.REJECTED);
        adjustment.setReviewedBy(reviewer);
        adjustment.setReviewedAt(Instant.now());
        adjustment.setReviewNotes(req == null ? null : req.reviewNotes());

        StockAdjustment saved = adjustmentRepository.save(adjustment);
        log.info("Stock adjustment rejected: id={}, by={}", adjustmentId, reviewer.getId());
        return adjustmentMapper.toResponse(saved);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private StockAdjustment getOrThrow(UUID id) {
        return adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.STOCK_ADJUSTMENT_NOT_FOUND,
                        "Stock adjustment with ID '%s' was not found".formatted(id)));
    }

    private StockAdjustment getPendingOrThrow(UUID id) {
        StockAdjustment adjustment = getOrThrow(id);
        if (!adjustment.isPending()) {
            throw new BusinessRuleException(
                    ErrorCode.STOCK_ADJUSTMENT_ALREADY_REVIEWED,
                    "Stock adjustment '%s' has already been %s".formatted(id, adjustment.getStatus()));
        }
        return adjustment;
    }
}
