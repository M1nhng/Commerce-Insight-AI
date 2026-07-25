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
 *
 * <p>Note: toEntity() and updateEntity() use default methods because
 * Lombok @Builder on a class with inherited BaseEntity fields is not
 * visible to MapStruct at annotation processing time.
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarehouseMapper {

    /** Maps a Warehouse entity to a WarehouseResponse DTO. */
    WarehouseResponse toResponse(Warehouse warehouse);

    /**
     * Builds a new Warehouse from a CreateWarehouseRequest.
     */
    default Warehouse toEntity(CreateWarehouseRequest request) {
        if (request == null) return null;
        return Warehouse.builder()
                .name(request.name())
                .code(request.code())
                .address(request.address())
                .city(request.city())
                .country(request.country())
                .active(true)
                .build();
    }

    /**
     * Updates an existing Warehouse entity with values from UpdateWarehouseRequest.
     * Only modifiable fields are updated; auditing fields are preserved.
     */
    default void updateEntity(UpdateWarehouseRequest request, Warehouse warehouse) {
        if (request == null || warehouse == null) return;
        warehouse.setName(request.name());
        warehouse.setCode(request.code());
        warehouse.setAddress(request.address());
        warehouse.setCity(request.city());
        warehouse.setCountry(request.country());
        warehouse.setActive(request.active());
    }
}
