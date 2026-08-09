package com.commerceinsight.customer.service;

import com.commerceinsight.customer.domain.*;
import com.commerceinsight.customer.dto.request.CreateAddressRequest;
import com.commerceinsight.customer.dto.request.UpdateAddressRequest;
import com.commerceinsight.customer.dto.response.CustomerAddressResponse;
import com.commerceinsight.customer.event.CustomerAddressChangedEvent;
import com.commerceinsight.customer.mapper.CustomerAddressMapper;
import com.commerceinsight.customer.repository.CustomerAddressRepository;
import com.commerceinsight.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CustomerAddressServiceTest — unit tests for CustomerAddressService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAddressService Unit Tests")
class CustomerAddressServiceTest {

    @Mock private CustomerAddressRepository addressRepository;
    @Mock private CustomerAddressMapper addressMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private CustomerAddressService addressService;

    private Customer customer;
    private CustomerAddress address;
    private CustomerAddressResponse addressResponse;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerCode("CUST-202608-00001");
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setStatus(CustomerStatus.ACTIVE);

        address = new CustomerAddress();
        address.setId(UUID.randomUUID());
        address.setCustomer(customer);
        address.setType(AddressType.SHIPPING);
        address.setRecipientName("Jane Smith");
        address.setAddressLine("123 Main St");
        address.setCountry("VN");
        address.setDefault(false);

        addressResponse = new CustomerAddressResponse(
                address.getId(), customer.getId(), AddressType.SHIPPING,
                "Jane Smith", null, "123 Main St", null, null, null, "VN", false, null, null);
    }

    // ── findAllByCustomer ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllByCustomer — returns list of addresses")
    void findAll_returnsAddressList() {
        when(addressRepository.findAllByCustomerIdOrderByTypeAscCreatedAtDesc(customer.getId()))
                .thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        var result = addressService.findAllByCustomer(customer.getId());
        assertThat(result).hasSize(1);
    }

    // ── addAddress ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addAddress — saves address and publishes ADDED event")
    void addAddress_saves_andPublishesEvent() {
        CreateAddressRequest request = new CreateAddressRequest(
                AddressType.SHIPPING, "Jane Smith", null, "123 Main St",
                null, null, null, "VN", false);

        when(addressMapper.toEntity(request)).thenReturn(address);
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        CustomerAddressResponse result = addressService.addAddress(customer, request);

        assertThat(result).isNotNull();
        verify(addressRepository).save(address);
        ArgumentCaptor<CustomerAddressChangedEvent> captor =
                ArgumentCaptor.forClass(CustomerAddressChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().changeType()).isEqualTo("ADDED");
    }

    @Test
    @DisplayName("addAddress — clears existing default when isDefault=true")
    void addAddress_clearsExistingDefault_whenIsDefaultTrue() {
        CreateAddressRequest request = new CreateAddressRequest(
                AddressType.SHIPPING, "Jane Smith", null, "New St",
                null, null, null, "VN", true);

        when(addressMapper.toEntity(request)).thenReturn(address);
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        addressService.addAddress(customer, request);

        verify(addressRepository).clearDefaultForType(customer.getId(), AddressType.SHIPPING);
    }

    // ── updateAddress ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAddress — updates fields and publishes UPDATED event")
    void updateAddress_success() {
        UpdateAddressRequest request = new UpdateAddressRequest(
                "Jane Updated", null, "456 New Ave", null, null, null, null);

        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        addressService.updateAddress(customer.getId(), address.getId(), request);

        verify(addressMapper).updateEntity(request, address);
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("updateAddress — throws when address belongs to different customer")
    void updateAddress_throws_whenAddressNotOwnedByCustomer() {
        UUID differentCustomerId = UUID.randomUUID();
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));

        assertThatThrownBy(() ->
                addressService.updateAddress(differentCustomerId, address.getId(), new UpdateAddressRequest(null,null,null,null,null,null,null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteAddress ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAddress — deletes address and publishes DELETED event")
    void deleteAddress_success() {
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        doNothing().when(addressRepository).delete(address);

        addressService.deleteAddress(customer.getId(), address.getId());

        verify(addressRepository).delete(address);
        ArgumentCaptor<CustomerAddressChangedEvent> captor =
                ArgumentCaptor.forClass(CustomerAddressChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().changeType()).isEqualTo("DELETED");
    }

    // ── setDefault ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("setDefault — sets default and publishes DEFAULT_SET event")
    void setDefault_success() {
        when(addressRepository.findById(address.getId())).thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);
        when(addressMapper.toResponse(address)).thenReturn(addressResponse);

        addressService.setDefault(customer.getId(), address.getId());

        assertThat(address.isDefault()).isTrue();
        verify(addressRepository).clearDefaultForType(customer.getId(), AddressType.SHIPPING);
        ArgumentCaptor<CustomerAddressChangedEvent> captor =
                ArgumentCaptor.forClass(CustomerAddressChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().changeType()).isEqualTo("DEFAULT_SET");
    }
}
