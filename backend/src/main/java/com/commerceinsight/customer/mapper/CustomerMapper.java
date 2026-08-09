package com.commerceinsight.customer.mapper;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.dto.request.CreateCustomerRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerRequest;
import com.commerceinsight.customer.dto.response.CustomerAddressResponse;
import com.commerceinsight.customer.dto.response.CustomerResponse;
import com.commerceinsight.customer.dto.response.CustomerSummaryResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * CustomerMapper — MapStruct mapper for Customer entity ↔ DTOs.
 *
 * <p>Notes:
 * <ul>
 *   <li>groupId and groupName are pulled from the lazy-loaded group association.</li>
 *   <li>fullName is derived from entity helper method.</li>
 *   <li>addresses list is mapped via CustomerAddressMapper (uses).</li>
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {CustomerAddressMapper.class}
)
public interface CustomerMapper {

    /** Map entity to full detail response. */
    @Mapping(target = "groupId",   source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "fullName",  expression = "java(customer.getFullName())")
    CustomerResponse toResponse(Customer customer);

    /** Map entity to lightweight summary response. */
    @Mapping(target = "groupId",   source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "fullName",  expression = "java(customer.getFullName())")
    CustomerSummaryResponse toSummary(Customer customer);

    /** Build a new Customer entity from a create request. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "status", expression = "java(com.commerceinsight.customer.domain.CustomerStatus.ACTIVE)")
    Customer toEntity(CreateCustomerRequest request);

    /** Update an existing Customer entity in-place from an update request. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    void updateEntity(UpdateCustomerRequest request, @MappingTarget Customer customer);
}
