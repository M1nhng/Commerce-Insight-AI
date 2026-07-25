package com.commerceinsight.inventory.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.inventory.domain.*;
import com.commerceinsight.inventory.dto.request.RequestStockAdjustmentRequest;
import com.commerceinsight.inventory.dto.request.ReviewStockAdjustmentRequest;
import com.commerceinsight.inventory.dto.response.StockAdjustmentResponse;
import com.commerceinsight.inventory.mapper.StockAdjustmentMapper;
import com.commerceinsight.inventory.repository.StockAdjustmentRepository;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StockAdjustmentServiceTest — unit tests for the StockAdjustmentService approval workflow.
 *
 * <p>Tests the state machine: PENDING → APPROVED | REJECTED
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockAdjustmentService Unit Tests")
class StockAdjustmentServiceTest {

    @Mock private StockAdjustmentRepository adjustmentRepository;
    @Mock private InventoryService inventoryService;
    @Mock private InventoryTransactionService transactionService;
    @Mock private StockAdjustmentMapper adjustmentMapper;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private StockAdjustmentService stockAdjustmentService;

    private User currentUser;
    private Inventory inventory;
    private StockAdjustment pendingAdjustment;
    private UUID adjustmentId;

    @BeforeEach
    void setUp() {
        adjustmentId = UUID.randomUUID();

        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setFirstName("Admin");
        currentUser.setLastName("User");

        Product product = Product.builder().name("Test Product").sku("SKU-001").build();
        Warehouse warehouse = Warehouse.builder().name("Main WH").code("WH-MAIN").build();

        inventory = Inventory.builder()
                .product(product).warehouse(warehouse)
                .quantity(100).reservedQuantity(0).build();

        pendingAdjustment = new StockAdjustment();
        pendingAdjustment.setInventory(inventory);
        pendingAdjustment.setProduct(product);
        pendingAdjustment.setWarehouse(warehouse);
        pendingAdjustment.setQuantityDelta(20);
        pendingAdjustment.setReason("Found extra units");
        pendingAdjustment.setStatus(AdjustmentStatus.PENDING);
        pendingAdjustment.setRequestedBy(currentUser);

        when(securityContextHelper.getCurrentUserOrThrow()).thenReturn(currentUser);
    }

    // ── request ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("request — creates PENDING adjustment without modifying inventory")
    void request_createsPendingAdjustment_withoutModifyingInventory() {
        RequestStockAdjustmentRequest req = new RequestStockAdjustmentRequest(
                UUID.randomUUID(), 20, "Found extra units");

        when(inventoryService.getOrThrow(req.inventoryId())).thenReturn(inventory);
        when(adjustmentRepository.save(any())).thenReturn(pendingAdjustment);
        when(adjustmentMapper.toResponse(pendingAdjustment)).thenReturn(mock(StockAdjustmentResponse.class));

        stockAdjustmentService.request(req);

        // Capture what was saved
        ArgumentCaptor<StockAdjustment> captor = ArgumentCaptor.forClass(StockAdjustment.class);
        verify(adjustmentRepository).save(captor.capture());

        StockAdjustment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AdjustmentStatus.PENDING);
        assertThat(saved.getQuantityDelta()).isEqualTo(20);

        // Inventory must NOT be touched
        assertThat(inventory.getQuantity()).isEqualTo(100);
        verify(transactionService, never()).record(any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve — applies stock change and sets status to APPROVED")
    void approve_appliesStockChange_andSetsApproved() {
        InventoryTransaction mockTxn = mock(InventoryTransaction.class);
        when(mockTxn.getId()).thenReturn(UUID.randomUUID());

        when(adjustmentRepository.findById(adjustmentId)).thenReturn(Optional.of(pendingAdjustment));
        when(transactionService.record(any(), eq(TransactionType.ADJUSTMENT),
                anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(mockTxn);
        when(adjustmentRepository.save(any())).thenReturn(pendingAdjustment);
        when(adjustmentMapper.toResponse(any())).thenReturn(mock(StockAdjustmentResponse.class));

        stockAdjustmentService.approve(adjustmentId, new ReviewStockAdjustmentRequest("LGTM"));

        // Stock should have been updated
        assertThat(inventory.getQuantity()).isEqualTo(120); // 100 + 20

        // Status should be APPROVED
        assertThat(pendingAdjustment.getStatus()).isEqualTo(AdjustmentStatus.APPROVED);
        assertThat(pendingAdjustment.getReviewedBy()).isEqualTo(currentUser);
        assertThat(pendingAdjustment.getReviewedAt()).isNotNull();
        assertThat(pendingAdjustment.getTransactionId()).isNotNull();
    }

    @Test
    @DisplayName("approve — throws BusinessRuleException when approval would make stock negative")
    void approve_throws_whenWouldMakeStockNegative() {
        // Delta of -150, but only 100 on hand
        pendingAdjustment.setQuantityDelta(-150);
        when(adjustmentRepository.findById(adjustmentId)).thenReturn(Optional.of(pendingAdjustment));

        assertThatThrownBy(() -> stockAdjustmentService.approve(adjustmentId, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negative stock");

        // Inventory untouched
        assertThat(inventory.getQuantity()).isEqualTo(100);
        verify(transactionService, never()).record(any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("approve — throws BusinessRuleException when adjustment is already reviewed")
    void approve_throws_whenAlreadyReviewed() {
        pendingAdjustment.setStatus(AdjustmentStatus.APPROVED); // already processed
        when(adjustmentRepository.findById(adjustmentId)).thenReturn(Optional.of(pendingAdjustment));

        assertThatThrownBy(() -> stockAdjustmentService.approve(adjustmentId, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reject — sets status to REJECTED without modifying inventory")
    void reject_setsRejected_withoutModifyingInventory() {
        when(adjustmentRepository.findById(adjustmentId)).thenReturn(Optional.of(pendingAdjustment));
        when(adjustmentRepository.save(any())).thenReturn(pendingAdjustment);
        when(adjustmentMapper.toResponse(any())).thenReturn(mock(StockAdjustmentResponse.class));

        stockAdjustmentService.reject(adjustmentId, new ReviewStockAdjustmentRequest("Not valid"));

        assertThat(pendingAdjustment.getStatus()).isEqualTo(AdjustmentStatus.REJECTED);
        assertThat(pendingAdjustment.getReviewedBy()).isEqualTo(currentUser);
        assertThat(pendingAdjustment.getReviewNotes()).isEqualTo("Not valid");

        // Inventory must NOT be touched
        assertThat(inventory.getQuantity()).isEqualTo(100);
        verify(transactionService, never()).record(any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("reject — throws BusinessRuleException when adjustment is already reviewed")
    void reject_throws_whenAlreadyReviewed() {
        pendingAdjustment.setStatus(AdjustmentStatus.REJECTED);
        when(adjustmentRepository.findById(adjustmentId)).thenReturn(Optional.of(pendingAdjustment));

        assertThatThrownBy(() -> stockAdjustmentService.reject(adjustmentId, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — throws ResourceNotFoundException when not found")
    void findById_throws_whenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(adjustmentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockAdjustmentService.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
