package com.commerceinsight.category.mapper;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.category.dto.response.CategoryResponse;
import com.commerceinsight.category.dto.response.CategoryTreeResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * CategoryMapper — MapStruct mapper for Category entity ↔ DTOs.
 *
 * <p>Architecture Rule: No business logic in mappers. Pure field mapping only.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CategoryMapper {

    /**
     * Map entity to flat {@link CategoryResponse}.
     * productCount is not on the entity — set separately in the service layer.
     */
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "productCount", ignore = true)
    CategoryResponse toResponse(Category category);

    /**
     * Map entity to {@link CategoryTreeResponse} with a provided children list and count.
     * The service layer builds the children list recursively and passes productCount.
     */
    @Mapping(target = "parentId", source = "category.parent.id")
    @Mapping(target = "productCount", source = "productCount")
    @Mapping(target = "children", source = "children")
    CategoryTreeResponse toTreeResponse(Category category, long productCount, List<CategoryTreeResponse> children);

    /**
     * Convenience overload with no children (leaf node).
     */
    default CategoryTreeResponse toTreeLeaf(Category category, long productCount) {
        return toTreeResponse(category, productCount, List.of());
    }
}
