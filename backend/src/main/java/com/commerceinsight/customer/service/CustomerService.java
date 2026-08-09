package com.commerceinsight.customer.service;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerGroup;
import com.commerceinsight.customer.domain.CustomerStatus;
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
import com.commerceinsight.customer.specification.CustomerSpecification;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CustomerService — business logic for customer management.
 *
 * <p>Business Rules enforced here:
 * <ol>
 *   <li>customerCode must be unique. Auto-generated if not provided.</li>
 *   <li>email must be unique when provided.</li>
 *   <li>Customers are soft-deleted — never physically removed.</li>
 *   <li>Group must exist before assignment.</li>
 *   <li>All status changes publish a domain event.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerGroupService customerGroupService;
    private final CustomerMapper customerMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> findAll(
            String keyword, CustomerStatus status, UUID groupId,
            Instant startDate, Instant endDate, Pageable pageable) {

        Specification<Customer> spec = CustomerSpecification.build(
                keyword, status, groupId, startDate, endDate);
        Page<Customer> page = customerRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(customerMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return customerMapper.toResponse(getOrThrow(id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        // Resolve / generate customerCode
        String code = resolveCode(request.customerCode());
        validateCodeUnique(code, null);

        // Validate email uniqueness
        if (StringUtils.hasText(request.email())) {
            if (customerRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException(
                        ErrorCode.CUSTOMER_EMAIL_ALREADY_EXISTS,
                        "Customer with email '%s' already exists".formatted(request.email()));
            }
        }

        Customer customer = customerMapper.toEntity(request);
        customer.setCustomerCode(code);

        // Resolve group if provided
        if (request.groupId() != null) {
            CustomerGroup group = customerGroupService.getOrThrow(request.groupId());
            customer.setGroup(group);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Customer created: id={}, code={}", saved.getId(), saved.getCustomerCode());

        eventPublisher.publishEvent(new CustomerCreatedEvent(
                saved.getId(), saved.getCustomerCode(), saved.getEmail()));

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(UUID id, UpdateCustomerRequest request) {
        Customer customer = getOrThrow(id);

        // Validate email uniqueness (only if changed)
        if (StringUtils.hasText(request.email())
                && !request.email().equals(customer.getEmail())) {
            if (customerRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new DuplicateResourceException(
                        ErrorCode.CUSTOMER_EMAIL_ALREADY_EXISTS,
                        "Customer with email '%s' already exists".formatted(request.email()));
            }
        }

        customerMapper.updateEntity(request, customer);

        // Resolve group assignment
        if (request.groupId() != null) {
            CustomerGroup group = customerGroupService.getOrThrow(request.groupId());
            customer.setGroup(group);
        }

        Customer saved = customerRepository.save(customer);
        log.info("Customer updated: id={}", id);

        eventPublisher.publishEvent(new CustomerUpdatedEvent(saved.getId(), saved.getCustomerCode()));

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = getOrThrow(id);
        customer.softDelete();
        customerRepository.save(customer);
        log.info("Customer soft-deleted: id={}, code={}", id, customer.getCustomerCode());
    }

    @Transactional
    public CustomerResponse updateStatus(UUID id, UpdateCustomerStatusRequest request) {
        Customer customer = getOrThrow(id);
        CustomerStatus oldStatus = customer.getStatus();
        CustomerStatus newStatus = request.status();

        customer.setStatus(newStatus);
        Customer saved = customerRepository.save(customer);
        log.info("Customer status changed: id={}, {} → {}", id, oldStatus, newStatus);

        eventPublisher.publishEvent(new CustomerStatusChangedEvent(
                saved.getId(), saved.getCustomerCode(), oldStatus, newStatus));

        return customerMapper.toResponse(saved);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    /**
     * Resolves the customer code: uses provided value or auto-generates one.
     */
    private String resolveCode(String provided) {
        if (StringUtils.hasText(provided)) {
            return provided.trim().toUpperCase();
        }
        String datePart = YearMonth.now().format(CODE_DATE_FMT);
        String seq = String.format("%05d", ThreadLocalRandom.current().nextInt(1, 99_999));
        return "CUST-" + datePart + "-" + seq;
    }

    /**
     * Validates customerCode uniqueness, excluding a given id for updates.
     */
    private void validateCodeUnique(String code, UUID excludeId) {
        boolean exists = (excludeId == null)
                ? customerRepository.existsByCustomerCode(code)
                : customerRepository.existsByCustomerCodeAndIdNot(code, excludeId);
        if (exists) {
            throw new DuplicateResourceException(
                    ErrorCode.CUSTOMER_CODE_ALREADY_EXISTS,
                    "Customer with code '%s' already exists".formatted(code));
        }
    }

    /**
     * Retrieves customer by ID or throws ResourceNotFoundException.
     * Package-internal so CustomerAddressService can resolve a customer.
     */
    public Customer getOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "Customer with ID '%s' was not found".formatted(id)));
    }
}
