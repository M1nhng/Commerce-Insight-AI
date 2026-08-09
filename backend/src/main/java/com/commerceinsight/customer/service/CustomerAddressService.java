package com.commerceinsight.customer.service;

import com.commerceinsight.customer.domain.AddressType;
import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerAddress;
import com.commerceinsight.customer.dto.request.CreateAddressRequest;
import com.commerceinsight.customer.dto.request.UpdateAddressRequest;
import com.commerceinsight.customer.dto.response.CustomerAddressResponse;
import com.commerceinsight.customer.event.CustomerAddressChangedEvent;
import com.commerceinsight.customer.mapper.CustomerAddressMapper;
import com.commerceinsight.customer.repository.CustomerAddressRepository;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CustomerAddressService — business logic for customer address management.
 *
 * <p>Business Rules:
 * <ol>
 *   <li>A customer may have at most ONE default SHIPPING address.</li>
 *   <li>A customer may have at most ONE default BILLING address.</li>
 *   <li>Setting a new default clears the previous default of the same type.</li>
 *   <li>Deleting an address does NOT auto-assign another default.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerAddressMapper addressMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> findAllByCustomer(UUID customerId) {
        return addressRepository.findAllByCustomerIdOrderByTypeAscCreatedAtDesc(customerId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Adds a new address to the customer.
     * If isDefault = true, clears any existing default of the same type first.
     */
    @Transactional
    public CustomerAddressResponse addAddress(Customer customer, CreateAddressRequest request) {
        CustomerAddress address = addressMapper.toEntity(request);
        address.setCustomer(customer);

        if (request.isDefault()) {
            clearExistingDefault(customer.getId(), request.type());
            address.setDefault(true);
        }

        CustomerAddress saved = addressRepository.save(address);
        log.info("Address added: customerId={}, type={}, id={}", customer.getId(), request.type(), saved.getId());

        eventPublisher.publishEvent(new CustomerAddressChangedEvent(
                customer.getId(), saved.getId(), saved.getType(), "ADDED"));

        return addressMapper.toResponse(saved);
    }

    /**
     * Updates an existing address (non-type fields only).
     */
    @Transactional
    public CustomerAddressResponse updateAddress(UUID customerId, UUID addressId, UpdateAddressRequest request) {
        CustomerAddress address = getOrThrow(addressId, customerId);
        addressMapper.updateEntity(request, address);
        CustomerAddress saved = addressRepository.save(address);
        log.info("Address updated: addressId={}", addressId);

        eventPublisher.publishEvent(new CustomerAddressChangedEvent(
                customerId, saved.getId(), saved.getType(), "UPDATED"));

        return addressMapper.toResponse(saved);
    }

    /**
     * Deletes an address. Hard delete — no soft delete on addresses.
     */
    @Transactional
    public void deleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = getOrThrow(addressId, customerId);
        AddressType type = address.getType();
        addressRepository.delete(address);
        log.info("Address deleted: customerId={}, addressId={}", customerId, addressId);

        eventPublisher.publishEvent(new CustomerAddressChangedEvent(
                customerId, addressId, type, "DELETED"));
    }

    /**
     * Sets an address as the default for its type.
     * Clears any existing default of the same type.
     */
    @Transactional
    public CustomerAddressResponse setDefault(UUID customerId, UUID addressId) {
        CustomerAddress address = getOrThrow(addressId, customerId);

        // Clear existing default for this type
        clearExistingDefault(customerId, address.getType());

        address.setDefault(true);
        CustomerAddress saved = addressRepository.save(address);
        log.info("Default address set: customerId={}, addressId={}, type={}", customerId, addressId, address.getType());

        eventPublisher.publishEvent(new CustomerAddressChangedEvent(
                customerId, saved.getId(), saved.getType(), "DEFAULT_SET"));

        return addressMapper.toResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void clearExistingDefault(UUID customerId, AddressType type) {
        addressRepository.clearDefaultForType(customerId, type);
    }

    private CustomerAddress getOrThrow(UUID addressId, UUID customerId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_ADDRESS_NOT_FOUND,
                        "Address with ID '%s' was not found".formatted(addressId)));

        // Security: ensure address belongs to this customer
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException(
                    ErrorCode.CUSTOMER_ADDRESS_NOT_FOUND,
                    "Address with ID '%s' does not belong to customer '%s'"
                            .formatted(addressId, customerId));
        }
        return address;
    }
}
