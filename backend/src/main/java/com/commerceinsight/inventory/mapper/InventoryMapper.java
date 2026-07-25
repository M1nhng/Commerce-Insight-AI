package com.commerceinsight.inventory.mapper;

import com.commerceinsight.inventory.domain.Inventory;
import com.commerceinsight.inventory.dto.response.InventoryResponse;
import org.mapstruct.*;

/**
 * InventoryMapper — MapStruct mapper for Inventory entity ↔ DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryMapper {

    @Mapping(target = "productId",      source = "product.id")
    @Mapping(target = "productName",    source = "product.name")
    @Mapping(target = "productSku",     source = "product.sku")
    @Mapping(target = "warehouseId",    source = "warehouse.id")
    @Mapping(target = "warehouseName",  source = "warehouse.name")
    @Mapping(target = "warehouseCode",  source = "warehouse.code")
    @Mapping(target = "availableQuantity", expression = "java(inventory.getAvailableQuantity())")
    @Mapping(target = "lowStock",       expression = "java(inventory.isLowStock())")
    InventoryResponse toResponse(Inventory inventory);
}
