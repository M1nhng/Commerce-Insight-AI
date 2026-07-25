package com.commerceinsight.inventory.mapper;

import com.commerceinsight.inventory.domain.Warehouse;
import com.commerceinsight.inventory.dto.request.CreateWarehouseRequest;
import com.commerceinsight.inventory.dto.request.UpdateWarehouseRequest;
import com.commerceinsight.inventory.dto.response.WarehouseResponse;
import org.mapstruct.*;

/**
 * WarehouseMapper — MapStruct mapper for Warehouse entity ↔ DTOs.
 *
 * <p>Architecture Rule: Never expose JPA entities to controllers.
 * All mappings go through this mapper.
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface WarehouseMapper {

    /** Maps a Warehouse entity to a WarehouseResponse DTO. */
    WarehouseResponse toResponse(Warehouse warehouse);

    /** Builds a new Warehouse entity from a CreateWarehouseRequest. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    Warehouse toEntity(CreateWarehouseRequest request);

    /** Updates an existing Warehouse entity in-place from an UpdateWarehouseRequest. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateWarehouseRequest request, @MappingTarget Warehouse warehouse);
}
