package com.commerceinsight.customer.repository;

import com.commerceinsight.customer.domain.CustomerSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * CustomerSegmentRepository — data access for {@link CustomerSegment}.
 */
@Repository
public interface CustomerSegmentRepository extends JpaRepository<CustomerSegment, UUID> {

    /** Check if a segment with the given code exists. */
    boolean existsByCode(String code);

    /** Check if any other segment (excluding id) has this code. */
    boolean existsByCodeAndIdNot(String code, UUID id);
}
