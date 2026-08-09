package com.commerceinsight.customer.repository;

import com.commerceinsight.customer.domain.CustomerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * CustomerGroupRepository — data access for {@link CustomerGroup}.
 */
@Repository
public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, UUID> {

    /** Check if a group with the given code already exists. */
    boolean existsByCode(String code);

    /** Check if any group other than the given id has this code. */
    boolean existsByCodeAndIdNot(String code, UUID id);

    /** Find a group by its unique code. */
    Optional<CustomerGroup> findByCode(String code);
}
