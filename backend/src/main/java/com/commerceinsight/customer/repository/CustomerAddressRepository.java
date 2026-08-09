package com.commerceinsight.customer.repository;

import com.commerceinsight.customer.domain.AddressType;
import com.commerceinsight.customer.domain.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CustomerAddressRepository — data access for {@link CustomerAddress}.
 */
@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    /** Get all addresses for a customer, ordered by type then creation date. */
    List<CustomerAddress> findAllByCustomerIdOrderByTypeAscCreatedAtDesc(UUID customerId);

    /** Find the current default address for a customer of a specific type. */
    Optional<CustomerAddress> findByCustomerIdAndTypeAndIsDefaultTrue(UUID customerId, AddressType type);

    /** Clear the default flag for all addresses of a given type for a customer. */
    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false " +
           "WHERE a.customer.id = :customerId AND a.type = :type")
    void clearDefaultForType(UUID customerId, AddressType type);

    /** Count addresses for a customer. */
    long countByCustomerId(UUID customerId);
}
