package com.commerceinsight.inventory.mapper;

import com.commerceinsight.inventory.domain.InventoryTransaction;
import com.commerceinsight.inventory.dto.response.InventoryTransactionResponse;
import org.mapstruct.*;

/**
 * InventoryTransactionMapper — MapStruct mapper for InventoryTransaction ↔ DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventoryTransactionMapper {

    @Mapping(target = "inventoryId",    source = "inventory.id")
    @Mapping(target = "productId",      source = "product.id")
    @Mapping(target = "productName",    source = "product.name")
    @Mapping(target = "productSku",     source = "product.sku")
    @Mapping(target = "warehouseId",    source = "warehouse.id")
    @Mapping(target = "warehouseName",  source = "warehouse.name")
    @Mapping(target = "performedById",  source = "performedBy.id")
    @Mapping(target = "performedByName",
             expression = "java(transaction.getPerformedBy() != null ? " +
                          "transaction.getPerformedBy().getFirstName() + ' ' + " +
                          "transaction.getPerformedBy().getLastName() : null)")
    InventoryTransactionResponse toResponse(InventoryTransaction transaction);
}
