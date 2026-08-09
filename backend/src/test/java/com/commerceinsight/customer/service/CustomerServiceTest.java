package com.commerceinsight.customer.service;

import com.commerceinsight.customer.domain.*;
import com.commerceinsight.customer.dto.request.CreateCustomerRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerStatusRequest;
import com.commerceinsight.customer.dto.response.CustomerResponse;
import com.commerceinsight.customer.dto.response.CustomerSummaryResponse;
import com.commerceinsight.customer.event.CustomerCreatedEvent;
import com.commerceinsight.customer.event.CustomerStatusChangedEvent;
import com.commerceinsight.customer.event.CustomerUpdatedEvent;
import com.commerceinsight.customer.mapper.CustomerMapper;
import com.commerceinsight.customer.repository.CustomerRepository;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CustomerServiceTest — unit tests for CustomerService business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerGroupService customerGroupService;
    @Mock private CustomerMapper customerMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private CustomerService customerService;

    private Customer customer;
    private CustomerResponse customerResponse;
    private CustomerSummaryResponse customerSummary;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerCode("CUST-202608-00001");
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("jane.smith@example.com");
        customer.setStatus(CustomerStatus.ACTIVE);

        customerResponse = new CustomerResponse(
                customer.getId(), "CUST-202608-00001", "Jane", "Smith", "Jane Smith",
                "jane.smith@example.com", null, null, null,
                CustomerStatus.ACTIVE, null, null, List.of(), Instant.now(), Instant.now());

        customerSummary = new CustomerSummaryResponse(
                customer.getId(), "CUST-202608-00001", "Jane", "Smith", "Jane Smith",
                "jane.smith@example.com", null, CustomerStatus.ACTIVE, null, null, Instant.now());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll — returns paginated summary list with specification")
    void findAll_returnsPaginatedList() {
        Page<Customer> page = new PageImpl<>(List.of(customer));
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(customerMapper.toSummary(customer)).thenReturn(customerSummary);

        PageResponse<CustomerSummaryResponse> result = customerService.findAll(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).customerCode()).isEqualTo("CUST-202608-00001");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — returns CustomerResponse when customer exists")
    void findById_returnsResponse_whenFound() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.findById(customer.getId());
        assertThat(result.id()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("findById — throws ResourceNotFoundException when not found")
    void findById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — creates customer with provided code and publishes CustomerCreatedEvent")
    void create_success_withProvidedCode() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-00001", "Jane", "Smith",
                "jane.smith@example.com", null, null, null, null);

        when(customerRepository.existsByCustomerCode("CUST-TEST-00001")).thenReturn(false);
        when(customerRepository.existsByEmail("jane.smith@example.com")).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.create(request);

        assertThat(result).isNotNull();
        verify(customerRepository).save(any());

        // customerCode on the saved entity is set to the uppercased provided code
        ArgumentCaptor<CustomerCreatedEvent> captor = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().customerCode()).isEqualTo("CUST-TEST-00001");
    }

    @Test
    @DisplayName("create — auto-generates code when customerCode is blank")
    void create_autoGeneratesCode_whenNotProvided() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                null, "Jane", "Smith", null, null, null, null, null);

        when(customerRepository.existsByCustomerCode(anyString())).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        customerService.create(request);

        // customer is a real object — verify via event that code starts with CUST-
        ArgumentCaptor<CustomerCreatedEvent> captor = ArgumentCaptor.forClass(CustomerCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().customerCode()).startsWith("CUST-");
    }

    @Test
    @DisplayName("create — throws DuplicateResourceException when customer code already exists")
    void create_throws_whenCodeAlreadyExists() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-DUPLICATE", "Jane", "Smith", null, null, null, null, null);

        when(customerRepository.existsByCustomerCode("CUST-DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CUST-DUPLICATE");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — throws DuplicateResourceException when email already exists")
    void create_throws_whenEmailAlreadyExists() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-00002", "John", "Doe",
                "jane.smith@example.com", null, null, null, null);

        when(customerRepository.existsByCustomerCode("CUST-TEST-00002")).thenReturn(false);
        when(customerRepository.existsByEmail("jane.smith@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("jane.smith@example.com");
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — assigns group when groupId provided and group exists")
    void create_assignsGroup_whenGroupIdProvided() {
        UUID groupId = UUID.randomUUID();
        CustomerGroup group = new CustomerGroup();
        group.setId(groupId);
        group.setCode("VIP");

        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-00003", "Jane", "Smith", null, null, null, null, groupId);

        when(customerRepository.existsByCustomerCode("CUST-TEST-00003")).thenReturn(false);
        when(customerGroupService.getOrThrow(groupId)).thenReturn(group);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(any())).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        customerService.create(request);

        verify(customerGroupService).getOrThrow(groupId);
        // customer is a real object — verify group was set via the entity field directly
        assertThat(customer.getGroup()).isEqualTo(group);
    }

    @Test
    @DisplayName("create — throws ResourceNotFoundException when groupId does not exist")
    void create_throws_whenGroupNotFound() {
        UUID groupId = UUID.randomUUID();
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-TEST-00004", "Jane", "Smith", null, null, null, null, groupId);

        when(customerRepository.existsByCustomerCode("CUST-TEST-00004")).thenReturn(false);
        // toEntity must return a valid customer so the service reaches the group lookup
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerGroupService.getOrThrow(groupId))
                .thenThrow(new ResourceNotFoundException(
                        com.commerceinsight.shared.exception.ErrorCode.CUSTOMER_GROUP_NOT_FOUND,
                        "Group not found"));

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — updates fields and publishes CustomerUpdatedEvent")
    void update_success() {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                "Jane Updated", "Smith", null, null, null, null, null);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        customerService.update(customer.getId(), request);

        verify(customerMapper).updateEntity(request, customer);
        verify(customerRepository).save(customer);

        ArgumentCaptor<CustomerUpdatedEvent> captor = ArgumentCaptor.forClass(CustomerUpdatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().customerId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("update — throws DuplicateResourceException when new email already used by another customer")
    void update_throws_whenEmailConflict() {
        UpdateCustomerRequest request = new UpdateCustomerRequest(
                null, null, "taken@example.com", null, null, null, null);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot("taken@example.com", customer.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> customerService.update(customer.getId(), request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepository, never()).save(any());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — soft-deletes customer by setting deletedAt")
    void delete_softDeletes() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        customerService.delete(customer.getId());

        assertThat(customer.getDeletedAt()).isNotNull();
        verify(customerRepository).save(customer);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus — changes status and publishes CustomerStatusChangedEvent")
    void updateStatus_success() {
        UpdateCustomerStatusRequest request = new UpdateCustomerStatusRequest(CustomerStatus.BLOCKED);

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        customerService.updateStatus(customer.getId(), request);

        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOCKED);

        ArgumentCaptor<CustomerStatusChangedEvent> captor =
                ArgumentCaptor.forClass(CustomerStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().oldStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(captor.getValue().newStatus()).isEqualTo(CustomerStatus.BLOCKED);
    }

    @Test
    @DisplayName("updateStatus — throws ResourceNotFoundException when customer not found")
    void updateStatus_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.updateStatus(id,
                new UpdateCustomerStatusRequest(CustomerStatus.INACTIVE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
