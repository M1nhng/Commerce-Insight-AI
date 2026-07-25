package com.commerceinsight.product.mapper;

import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.domain.ProductImage;
import com.commerceinsight.product.dto.response.ProductResponse;
import com.commerceinsight.product.dto.response.ProductSummaryResponse;
import org.mapstruct.*;

/**
 * ProductMapper — MapStruct mapper for Product entity ↔ DTOs.
 *
 * <p>Architecture Rule: Pure field mapping only. No business logic.
 * stockQuantity is always set to 0 here — Sprint 7 (Inventory) will override.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    /**
     * Map entity to lightweight {@link ProductSummaryResponse}.
     * Category name and ID are pulled from the lazy-loaded Category association.
     */
    @Mapping(target = "categoryId",   source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "stockQuantity", constant = "0")
    ProductSummaryResponse toSummary(Product product);

    /**
     * Map entity to full {@link ProductResponse}.
     */
    @Mapping(target = "categoryId",   source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "stockQuantity", constant = "0")
    @Mapping(target = "images",       source = "images")
    ProductResponse toResponse(Product product);

    /**
     * Map a ProductImage entity to the embedded DTO.
     */
    ProductResponse.ProductImageResponse toImageResponse(ProductImage image);
}
