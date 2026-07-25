package com.commerceinsight.inventory.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.inventory.domain.*;
import com.commerceinsight.inventory.dto.request.AdjustStockRequest;
import com.commerceinsight.inventory.dto.request.TransferStockRequest;
import com.commerceinsight.inventory.mapper.InventoryMapper;
import com.commerceinsight.inventory.mapper.InventoryTransactionMapper;
import com.commerceinsight.inventory.repository.InventoryRepository;
import com.commerceinsight.inventory.repository.InventoryTransactionRepository;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.repository.ProductRepository;
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
 * InventoryServiceTest — unit tests for InventoryService business logic.
 *
 * <p>Key invariants tested:
 * <ul>
 *   <li>Stock NEVER goes below zero</li>
 *   <li>Every adjustment records an InventoryTransaction</li>
 *   <li>Transfers are rejected if same source/destination warehouse</li>
 *   <li>Transfers fail when source has insufficient available stock</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private WarehouseService warehouseService;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryTransactionService transactionService;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private InventoryTransactionMapper transactionMapper;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private InventoryService inventoryService;

    private User currentUser;
    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;
    private UUID inventoryId;

    @BeforeEach
    void setUp() {
        inventoryId = UUID.randomUUID();

        currentUser = new User();
        currentUser.setId(UUID.randomUUID());

        product = Product.builder()
                .name("Test Product").sku("SKU-001").build();

        warehouse = Warehouse.builder()
                .name("Main WH").code("WH-MAIN").active(true).build();

        inventory = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .quantity(100)
                .reservedQuantity(10)
                .lowStockThreshold(10)
                .build();

        when(securityContextHelper.getCurrentUserOrThrow()).thenReturn(currentUser);
    }

    // ── adjustStock ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("adjustStock — adds stock and records ADJUSTMENT transaction")
    void adjustStock_addsStock_andRecordsTransaction() {
        AdjustStockRequest request = new AdjustStockRequest(50, "Replenishment", null);
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(null);

        inventoryService.adjustStock(inventoryId, request);

        assertThat(inventory.getQuantity()).isEqualTo(150);
        verify(transactionService).record(
                eq(inventory), eq(TransactionType.ADJUSTMENT),
                eq(50), eq(100), eq(150),
                eq(currentUser), isNull(), anyString());
    }

    @Test
    @DisplayName("adjustStock — removes stock successfully when stock is sufficient")
    void adjustStock_removesStock_whenSufficient() {
        AdjustStockRequest request = new AdjustStockRequest(-30, "Damaged goods", null);
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(null);

        inventoryService.adjustStock(inventoryId, request);

        assertThat(inventory.getQuantity()).isEqualTo(70);
        verify(transactionService).record(
                eq(inventory), eq(TransactionType.ADJUSTMENT),
                eq(-30), eq(100), eq(70),
                eq(currentUser), isNull(), anyString());
    }

    @Test
    @DisplayName("adjustStock — throws BusinessRuleException when result would be negative")
    void adjustStock_throws_whenResultWouldBeNegative() {
        // Only 100 units, trying to remove 150
        AdjustStockRequest request = new AdjustStockRequest(-150, "Too much", null);
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.adjustStock(inventoryId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negative stock");

        // No save, no transaction recorded
        verify(inventoryRepository, never()).save(any());
        verify(transactionService, never()).record(any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("adjustStock — also updates lowStockThreshold when provided")
    void adjustStock_updatesThreshold_whenProvided() {
        AdjustStockRequest request = new AdjustStockRequest(0, "Threshold update", 5);
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(inventoryMapper.toResponse(any())).thenReturn(null);

        inventoryService.adjustStock(inventoryId, request);

        assertThat(inventory.getLowStockThreshold()).isEqualTo(5);
    }

    @Test
    @DisplayName("adjustStock — throws ResourceNotFoundException when inventory not found")
    void adjustStock_throws_whenInventoryNotFound() {
        when(inventoryRepository.findById(inventoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.adjustStock(inventoryId, new AdjustStockRequest(10, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── transfer ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("transfer — throws BusinessRuleException when source and destination are the same")
    void transfer_throws_whenSameWarehouse() {
        UUID warehouseId = UUID.randomUUID();
        TransferStockRequest request = new TransferStockRequest(
                UUID.randomUUID(), warehouseId, warehouseId, 10, null);

        assertThatThrownBy(() -> inventoryService.transfer(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Source and destination warehouses must be different");
    }

    @Test
    @DisplayName("transfer — throws BusinessRuleException when insufficient available stock")
    void transfer_throws_whenInsufficientAvailableStock() {
        UUID productId = UUID.randomUUID();
        UUID sourceId  = UUID.randomUUID();
        UUID destId    = UUID.randomUUID();

        // inventory has 100 total, 10 reserved → 90 available
        // Trying to transfer 95 (> 90 available)
        Inventory sourceInventory = Inventory.builder()
                .product(product).warehouse(warehouse)
                .quantity(100).reservedQuantity(10).build();

        Warehouse destWarehouse = Warehouse.builder().name("East WH").code("WH-EAST").build();

        when(warehouseService.getOrThrow(sourceId)).thenReturn(warehouse);
        when(warehouseService.getOrThrow(destId)).thenReturn(destWarehouse);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdAndWarehouseId(productId, sourceId))
                .thenReturn(Optional.of(sourceInventory));

        TransferStockRequest request = new TransferStockRequest(productId, sourceId, destId, 95, null);

        assertThatThrownBy(() -> inventoryService.transfer(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient available stock");
    }

    @Test
    @DisplayName("transfer — successfully deducts source and adds to destination with two transactions")
    void transfer_success_createsTwoTransactions() {
        UUID productId = UUID.randomUUID();
        UUID sourceId  = UUID.randomUUID();
        UUID destId    = UUID.randomUUID();

        Warehouse destWarehouse = Warehouse.builder().name("East WH").code("WH-EAST").build();

        Inventory sourceInv = Inventory.builder()
                .product(product).warehouse(warehouse)
                .quantity(100).reservedQuantity(0).build();
        Inventory destInv = Inventory.builder()
                .product(product).warehouse(destWarehouse)
                .quantity(20).reservedQuantity(0).build();

        InventoryTransaction mockTxn = mock(InventoryTransaction.class);
        when(mockTxn.getId()).thenReturn(UUID.randomUUID());

        when(warehouseService.getOrThrow(sourceId)).thenReturn(warehouse);
        when(warehouseService.getOrThrow(destId)).thenReturn(destWarehouse);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdAndWarehouseId(productId, sourceId))
                .thenReturn(Optional.of(sourceInv));
        when(inventoryRepository.findByProductIdAndWarehouseId(productId, destId))
                .thenReturn(Optional.of(destInv));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArguments()[0]);
        when(transactionService.record(eq(sourceInv), eq(TransactionType.TRANSFER_OUT),
                anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(mockTxn);

        TransferStockRequest request = new TransferStockRequest(productId, sourceId, destId, 30, "Transfer");
        inventoryService.transfer(request);

        // Source should be reduced, dest should be increased
        assertThat(sourceInv.getQuantity()).isEqualTo(70);
        assertThat(destInv.getQuantity()).isEqualTo(50);

        // TRANSFER_OUT recorded for source
        verify(transactionService).record(eq(sourceInv), eq(TransactionType.TRANSFER_OUT),
                eq(-30), eq(100), eq(70), eq(currentUser), isNull(), anyString());

        // TRANSFER_IN recorded for destination
        verify(transactionService).record(eq(destInv), eq(TransactionType.TRANSFER_IN),
                eq(30), eq(20), eq(50), eq(currentUser), any(UUID.class), anyString());
    }

    @Test
    @DisplayName("transfer — creates destination inventory row when it does not exist")
    void transfer_createsDestInventory_whenMissing() {
        UUID productId = UUID.randomUUID();
        UUID sourceId  = UUID.randomUUID();
        UUID destId    = UUID.randomUUID();

        Warehouse destWarehouse = Warehouse.builder().name("East WH").code("WH-EAST").build();

        Inventory sourceInv = Inventory.builder()
                .product(product).warehouse(warehouse)
                .quantity(50).reservedQuantity(0).build();

        InventoryTransaction mockTxn = mock(InventoryTransaction.class);
        when(mockTxn.getId()).thenReturn(UUID.randomUUID());

        when(warehouseService.getOrThrow(sourceId)).thenReturn(warehouse);
        when(warehouseService.getOrThrow(destId)).thenReturn(destWarehouse);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdAndWarehouseId(productId, sourceId))
                .thenReturn(Optional.of(sourceInv));
        when(inventoryRepository.findByProductIdAndWarehouseId(productId, destId))
                .thenReturn(Optional.empty()); // no dest inventory
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArguments()[0]);
        when(transactionService.record(any(), eq(TransactionType.TRANSFER_OUT),
                anyInt(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(mockTxn);

        TransferStockRequest request = new TransferStockRequest(productId, sourceId, destId, 10, null);
        inventoryService.transfer(request);

        // Save called at least twice (source + new dest)
        verify(inventoryRepository, atLeast(2)).save(any());
    }
}
