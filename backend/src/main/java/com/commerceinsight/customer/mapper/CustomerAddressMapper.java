package com.commerceinsight.customer.mapper;

import com.commerceinsight.customer.domain.CustomerAddress;
import com.commerceinsight.customer.dto.request.CreateAddressRequest;
import com.commerceinsight.customer.dto.request.UpdateAddressRequest;
import com.commerceinsight.customer.dto.response.CustomerAddressResponse;
import org.mapstruct.*;

/**
 * CustomerAddressMapper — MapStruct mapper for CustomerAddress entity ↔ DTOs.
 *
 * <p>Note: The entity uses a primitive boolean field named {@code isDefault}.
 * Lombok generates {@code isDefault()} getter and {@code setDefault()} setter.
 * MapStruct resolves this as target property "default" (stripped is-prefix).
 * The DTO record uses {@code isDefault} which MapStruct maps as property "isDefault".
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerAddressMapper {

    /** Map entity to response DTO. */
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "isDefault",  source = "default")
    CustomerAddressResponse toResponse(CustomerAddress address);

    /** Build a new CustomerAddress from a create request. */
    @Mapping(target = "id",       ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "default",  source = "isDefault")
    @Mapping(target = "country",  expression = "java(request.country() != null ? request.country() : \"VN\")")
    CustomerAddress toEntity(CreateAddressRequest request);

    /** Update an existing CustomerAddress in-place (type and isDefault not changed here). */
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "customer",  ignore = true)
    @Mapping(target = "type",      ignore = true)
    @Mapping(target = "default",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateAddressRequest request, @MappingTarget CustomerAddress address);
}
