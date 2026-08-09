package com.commerceinsight.customer.service;

import com.commerceinsight.customer.domain.CustomerGroup;
import com.commerceinsight.customer.domain.GroupStatus;
import com.commerceinsight.customer.dto.request.CreateCustomerGroupRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerGroupRequest;
import com.commerceinsight.customer.dto.response.CustomerGroupResponse;
import com.commerceinsight.customer.mapper.CustomerGroupMapper;
import com.commerceinsight.customer.repository.CustomerGroupRepository;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CustomerGroupServiceTest — unit tests for CustomerGroupService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerGroupService Unit Tests")
class CustomerGroupServiceTest {

    @Mock private CustomerGroupRepository groupRepository;
    @Mock private CustomerGroupMapper groupMapper;

    @InjectMocks private CustomerGroupService groupService;

    private CustomerGroup group;
    private CustomerGroupResponse groupResponse;

    @BeforeEach
    void setUp() {
        group = new CustomerGroup();
        group.setId(UUID.randomUUID());
        group.setCode("VIP");
        group.setName("VIP Customers");
        group.setStatus(GroupStatus.ACTIVE);

        groupResponse = new CustomerGroupResponse(
                group.getId(), "VIP", "VIP Customers", null, GroupStatus.ACTIVE, null, null);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll — returns paginated group responses")
    void findAll_returnsPaginatedList() {
        Page<CustomerGroup> page = new PageImpl<>(List.of(group));
        when(groupRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        var result = groupService.findAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("VIP");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — returns response when group exists")
    void findById_returnsResponse_whenExists() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        CustomerGroupResponse result = groupService.findById(group.getId());
        assertThat(result.code()).isEqualTo("VIP");
    }

    @Test
    @DisplayName("findById — throws ResourceNotFoundException when not found")
    void findById_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(groupRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — creates group when code is unique")
    void create_success_whenCodeUnique() {
        CreateCustomerGroupRequest request = new CreateCustomerGroupRequest(
                "VIP", "VIP Customers", null, GroupStatus.ACTIVE);

        when(groupRepository.existsByCode("VIP")).thenReturn(false);
        when(groupMapper.toEntity(request)).thenReturn(group);
        when(groupRepository.save(group)).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        CustomerGroupResponse result = groupService.create(request);

        assertThat(result).isEqualTo(groupResponse);
        verify(groupRepository).save(group);
    }

    @Test
    @DisplayName("create — throws DuplicateResourceException when code exists")
    void create_throws_whenCodeAlreadyExists() {
        CreateCustomerGroupRequest request = new CreateCustomerGroupRequest(
                "VIP", "VIP Customers", null, null);

        when(groupRepository.existsByCode("VIP")).thenReturn(true);

        assertThatThrownBy(() -> groupService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("VIP");
        verify(groupRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — updates group fields when group exists")
    void update_success() {
        UpdateCustomerGroupRequest request = new UpdateCustomerGroupRequest(
                "Super VIP", "Top tier customers", GroupStatus.ACTIVE);

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupRepository.save(group)).thenReturn(group);
        when(groupMapper.toResponse(group)).thenReturn(groupResponse);

        CustomerGroupResponse result = groupService.update(group.getId(), request);

        assertThat(result).isNotNull();
        verify(groupMapper).updateEntity(request, group);
        verify(groupRepository).save(group);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — deletes group when exists")
    void delete_success() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        doNothing().when(groupRepository).delete(group);

        groupService.delete(group.getId());

        verify(groupRepository).delete(group);
    }

    @Test
    @DisplayName("delete — throws ResourceNotFoundException when not found")
    void delete_throws_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(groupRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(groupRepository, never()).delete(any());
    }
}
