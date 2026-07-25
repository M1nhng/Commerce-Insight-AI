package com.commerceinsight.inventory.mapper;

import com.commerceinsight.inventory.domain.StockAdjustment;
import com.commerceinsight.inventory.dto.response.StockAdjustmentResponse;
import org.mapstruct.*;

/**
 * StockAdjustmentMapper — MapStruct mapper for StockAdjustment ↔ DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StockAdjustmentMapper {

    @Mapping(target = "inventoryId",     source = "inventory.id")
    @Mapping(target = "productId",       source = "product.id")
    @Mapping(target = "productName",     source = "product.name")
    @Mapping(target = "productSku",      source = "product.sku")
    @Mapping(target = "warehouseId",     source = "warehouse.id")
    @Mapping(target = "warehouseName",   source = "warehouse.name")
    @Mapping(target = "requestedById",   source = "requestedBy.id")
    @Mapping(target = "requestedByName",
             expression = "java(adj.getRequestedBy() != null ? " +
                          "adj.getRequestedBy().getFirstName() + ' ' + " +
                          "adj.getRequestedBy().getLastName() : null)")
    @Mapping(target = "reviewedById",    source = "reviewedBy.id")
    @Mapping(target = "reviewedByName",
             expression = "java(adj.getReviewedBy() != null ? " +
                          "adj.getReviewedBy().getFirstName() + ' ' + " +
                          "adj.getReviewedBy().getLastName() : null)")
    StockAdjustmentResponse toResponse(StockAdjustment adj);
}
