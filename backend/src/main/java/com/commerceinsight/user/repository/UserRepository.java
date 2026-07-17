package com.commerceinsight.user.repository;

import com.commerceinsight.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    /**
     * Find locked users whose account was locked before the given threshold.
     * Used by the auto-unlock scheduler to release accounts locked > 15 minutes ago.
     *
     * <p>Note: Uses updatedAt as the lock timestamp since locked is set and saved
     * at the same time as the updatedAt field is updated by Spring Data Auditing.
     *
     * @param threshold auto-unlock users locked before this instant
     * @return list of users whose lock should be lifted
     */
    @Query("SELECT u FROM User u WHERE u.locked = true AND u.updatedAt < :threshold")
    List<User> findLockedUsersLockedBefore(@Param("threshold") Instant threshold);
}
