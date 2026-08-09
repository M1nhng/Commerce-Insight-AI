package com.commerceinsight.customer.repository;

import com.commerceinsight.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * CustomerRepository — data access for {@link Customer}.
 *
 * <p>Extends JpaSpecificationExecutor for dynamic search/filter queries.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>,
        JpaSpecificationExecutor<Customer> {

    /** Check if a customer with the given code exists (excluding deleted). */
    boolean existsByCustomerCode(String customerCode);

    /** Check if any other customer (excluding id) has this code. */
    boolean existsByCustomerCodeAndIdNot(String customerCode, UUID id);

    /** Check if a customer with the given email exists (excluding deleted). */
    boolean existsByEmail(String email);

    /** Check if any other customer (excluding id) has this email. */
    boolean existsByEmailAndIdNot(String email, UUID id);

    /** Find by customer code. */
    Optional<Customer> findByCustomerCode(String customerCode);
}
