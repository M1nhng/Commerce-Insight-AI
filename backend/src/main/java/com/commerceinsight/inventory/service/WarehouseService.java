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
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * WarehouseService — business logic for warehouse management.
 *
 * <p>Architecture Rule: All business logic lives here, not in the controller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseMapper warehouseMapper;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<WarehouseResponse> findAll(Pageable pageable) {
        Page<Warehouse> page = warehouseRepository.findAll(pageable);
        return PageResponse.from(page.map(warehouseMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public WarehouseResponse findById(UUID id) {
        return warehouseMapper.toResponse(getOrThrow(id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        validateCodeUnique(request.code(), null);

        Warehouse warehouse = warehouseMapper.toEntity(request);
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created: id={}, code={}", saved.getId(), saved.getCode());
        return warehouseMapper.toResponse(saved);
    }

    @Transactional
    public WarehouseResponse update(UUID id, UpdateWarehouseRequest request) {
        Warehouse warehouse = getOrThrow(id);
        validateCodeUnique(request.code(), id);

        warehouseMapper.updateEntity(request, warehouse);
        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse updated: id={}", saved.getId());
        return warehouseMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Warehouse warehouse = getOrThrow(id);

        // Guard: do not delete a warehouse that still holds inventory
        boolean hasInventory = inventoryRepository.findAllByWarehouseId(id)
                .stream().anyMatch(inv -> inv.getQuantity() > 0);
        if (hasInventory) {
            throw new BusinessRuleException(
                    ErrorCode.WAREHOUSE_HAS_INVENTORY,
                    "Cannot delete warehouse '%s': it still has inventory. Transfer or zero out stock first."
                            .formatted(warehouse.getName()));
        }

        warehouse.softDelete();
        warehouseRepository.save(warehouse);
        log.info("Warehouse soft-deleted: id={}", id);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    public Warehouse getOrThrow(UUID id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.WAREHOUSE_NOT_FOUND,
                        "Warehouse with ID '%s' was not found".formatted(id)));
    }

    private void validateCodeUnique(String code, UUID excludeId) {
        boolean exists = (excludeId == null)
                ? warehouseRepository.existsByCode(code)
                : warehouseRepository.existsByCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new DuplicateResourceException(
                    ErrorCode.WAREHOUSE_CODE_ALREADY_EXISTS,
                    "Warehouse with code '%s' already exists".formatted(code));
        }
    }
}
