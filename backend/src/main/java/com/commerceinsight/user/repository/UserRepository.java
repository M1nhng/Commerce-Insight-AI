package com.commerceinsight.user.repository;

import com.commerceinsight.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository — data access for the {@link User} entity.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Only the UserService and security infrastructure may inject this repository.</li>
 *   <li>No other module may inject this repository directly.</li>
 *   <li>Soft-deleted users are automatically excluded by @SQLRestriction on User entity.</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User> {

    /**
     * Find an active (non-deleted) user by their email address.
     * Used during login and email uniqueness checks.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered for any non-deleted user.
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);
}
