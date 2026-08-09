package com.commerceinsight.customer.service;

import com.commerceinsight.customer.domain.CustomerGroup;
import com.commerceinsight.customer.dto.request.CreateCustomerGroupRequest;
import com.commerceinsight.customer.dto.request.UpdateCustomerGroupRequest;
import com.commerceinsight.customer.dto.response.CustomerGroupResponse;
import com.commerceinsight.customer.mapper.CustomerGroupMapper;
import com.commerceinsight.customer.repository.CustomerGroupRepository;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CustomerGroupService — business logic for customer group management.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>Group code must be globally unique.</li>
 *   <li>Groups with assigned customers can be deleted — FK SET NULL on customer side.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerGroupService {

    private final CustomerGroupRepository groupRepository;
    private final CustomerGroupMapper groupMapper;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CustomerGroupResponse> findAll(Pageable pageable) {
        return groupRepository.findAll(pageable).map(groupMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerGroupResponse findById(UUID id) {
        return groupMapper.toResponse(getOrThrow(id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerGroupResponse create(CreateCustomerGroupRequest request) {
        if (groupRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException(
                    ErrorCode.CUSTOMER_GROUP_CODE_ALREADY_EXISTS,
                    "Customer group with code '%s' already exists".formatted(request.code()));
        }

        CustomerGroup group = groupMapper.toEntity(request);
        CustomerGroup saved = groupRepository.save(group);
        log.info("CustomerGroup created: id={}, code={}", saved.getId(), saved.getCode());
        return groupMapper.toResponse(saved);
    }

    @Transactional
    public CustomerGroupResponse update(UUID id, UpdateCustomerGroupRequest request) {
        CustomerGroup group = getOrThrow(id);
        groupMapper.updateEntity(request, group);
        CustomerGroup saved = groupRepository.save(group);
        log.info("CustomerGroup updated: id={}", saved.getId());
        return groupMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        CustomerGroup group = getOrThrow(id);
        groupRepository.delete(group);
        log.info("CustomerGroup deleted: id={}, code={}", id, group.getCode());
    }

    // ── Package-internal helper ───────────────────────────────────────────────

    /**
     * Used by CustomerService to resolve group by ID before assigning.
     */
    public CustomerGroup getOrThrow(UUID id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_GROUP_NOT_FOUND,
                        "Customer group with ID '%s' was not found".formatted(id)));
    }
}
