package com.commerceinsight.inventory.service;

import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.inventory.domain.Warehouse;
import com.commerceinsight.inventory.dto.request.CreateWarehouseRequest;
import com.commerceinsight.inventory.dto.request.UpdateWarehouseRequest;
import com.commerceinsight.inventory.dto.response.WarehouseResponse;
import com.commerceinsight.inventory.mapper.WarehouseMapper;
import com.commerceinsight.inventory.repository.InventoryRepository;
import com.commerceinsight.inventory.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WarehouseServiceTest — unit tests for WarehouseService business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WarehouseService Unit Tests")
class WarehouseServiceTest {

    @Mock private WarehouseRepository warehouseRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseMapper warehouseMapper;

    @InjectMocks private WarehouseService warehouseService;

    private Warehouse warehouse;
    private WarehouseResponse warehouseResponse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .name("Main Warehouse")
                .code("WH-MAIN")
                .active(true)
                .build();

        warehouseResponse = new WarehouseResponse(
                UUID.randomUUID(), "Main Warehouse", "WH-MAIN",
                null, null, null, true, null, null);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll — returns paginated WarehouseResponse list")
    void findAll_returnsPaginatedList() {
        Page<Warehouse> page = new PageImpl<>(List.of(warehouse));
        when(warehouseRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        var result = warehouseService.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — returns response when warehouse exists")
    void findById_returnsResponse_whenExists() {
        UUID id = UUID.randomUUID();
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouse));
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        WarehouseResponse result = warehouseService.findById(id);
        assertThat(result).isEqualTo(warehouseResponse);
    }

    @Test
    @DisplayName("findById — throws ResourceNotFoundException when warehouse does not exist")
    void findById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(warehouseRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — creates warehouse when code is unique")
    void create_success_whenCodeIsUnique() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Main Warehouse", "WH-MAIN", null, null, null);

        when(warehouseRepository.existsByCode("WH-MAIN")).thenReturn(false);
        when(warehouseMapper.toEntity(request)).thenReturn(warehouse);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        WarehouseResponse result = warehouseService.create(request);
        assertThat(result).isEqualTo(warehouseResponse);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    @DisplayName("create — throws DuplicateResourceException when code already exists")
    void create_throws_whenCodeAlreadyExists() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                "Other Warehouse", "WH-MAIN", null, null, null);

        when(warehouseRepository.existsByCode("WH-MAIN")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(warehouseRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — updates warehouse fields successfully")
    void update_success() {
        UUID id = UUID.randomUUID();
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(
                "Updated Name", "WH-MAIN", "123 Main St", "Hanoi", "VN", true);

        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByCodeAndIdNot("WH-MAIN", id)).thenReturn(false);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.toResponse(warehouse)).thenReturn(warehouseResponse);

        WarehouseResponse result = warehouseService.update(id, request);
        assertThat(result).isNotNull();
        verify(warehouseMapper).updateEntity(request, warehouse);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — soft-deletes when warehouse has no stock")
    void delete_softDeletes_whenNoStock() {
        UUID id = UUID.randomUUID();
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findAllByWarehouseId(id)).thenReturn(Collections.emptyList());

        warehouseService.delete(id);

        assertThat(warehouse.getDeletedAt()).isNotNull();
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    @DisplayName("delete — throws BusinessRuleException when warehouse has stock")
    void delete_throws_whenWarehouseHasStock() {
        UUID id = UUID.randomUUID();
        com.commerceinsight.inventory.domain.Inventory inv =
                mock(com.commerceinsight.inventory.domain.Inventory.class);
        when(inv.getQuantity()).thenReturn(5);

        when(warehouseRepository.findById(id)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findAllByWarehouseId(id)).thenReturn(List.of(inv));

        assertThatThrownBy(() -> warehouseService.delete(id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("still has inventory");
        verify(warehouseRepository, never()).save(any());
    }
}
