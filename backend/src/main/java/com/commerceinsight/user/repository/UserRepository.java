package com.commerceinsight.user.repository;

import com.commerceinsight.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository — data access for the {@link User} entity.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Only the UserService may inject this repository.</li>
 *   <li>SecurityContextHelper may also inject this for user resolution.</li>
 *   <li>No other module may inject this repository.</li>
 *   <li>Soft-deleted users are automatically excluded by @SQLRestriction.</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User> {

    /**
     * Find an active user by their email address.
     * Used during login and email uniqueness checks.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered (for any non-deleted user).
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by their UUID string — used by UserDetailsService.
     * Converts the string to UUID to enable index-based lookup.
     */
    @Query("SELECT u FROM User u WHERE CAST(u.id AS string) = :idStr")
    Optional<User> findByIdString(@Param("idStr") String idStr);
}
