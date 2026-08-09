package com.commerceinsight.customer.mapper;

import com.commerceinsight.customer.domain.CustomerGroup;
import com.commerceinsight.customer.domain.GroupStatus;
import com.commerceinsight.customer.dto.request.CreateCustomerGroupRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerGroupRequest;
import com.commerceinsight.customer.dto.response.CustomerGroupResponse;
import org.mapstruct.*;

/**
 * CustomerGroupMapper — MapStruct mapper for CustomerGroup entity ↔ DTOs.
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CustomerGroupMapper {

    /** Map entity to response DTO. */
    CustomerGroupResponse toResponse(CustomerGroup group);

    /** Build a new CustomerGroup entity from a create request. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(request.status() != null ? request.status() : com.commerceinsight.customer.domain.GroupStatus.ACTIVE)")
    CustomerGroup toEntity(CreateCustomerGroupRequest request);

    /** Update an existing CustomerGroup entity in-place. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateCustomerGroupRequest request, @MappingTarget CustomerGroup group);
}
