package com.commerceinsight.security;

import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * SecurityContextHelper — provides convenient access to the currently authenticated user.
 *
 * <p>Usage in service layer:
 * <pre>
 * User currentUser = securityContextHelper.getCurrentUserOrThrow();
 * UUID currentUserId = securityContextHelper.getCurrentUserId();
 * </pre>
 *
 * <p>Architecture Rule: Only services should inject this. Controllers must NOT
 * access security context directly — pass the resolved user down from the service.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextHelper {

    private final UserRepository userRepository;

    /**
     * Get the UUID of the currently authenticated user from the SecurityContext.
     *
     * @return the authenticated user's UUID
     * @throws IllegalStateException if no authentication exists
     */
    public UUID getCurrentUserId() {
        Authentication auth = getAuthentication();
        return UUID.fromString(auth.getName());
    }

    /**
     * Get the currently authenticated {@link User} entity from the database.
     *
     * @return Optional containing the user, or empty if not found
     */
    public Optional<User> getCurrentUser() {
        try {
            UUID userId = getCurrentUserId();
            return userRepository.findById(userId);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Get the currently authenticated user or throw if not authenticated.
     *
     * @return the authenticated User entity
     * @throws IllegalStateException if the user cannot be resolved
     */
    public User getCurrentUserOrThrow() {
        UUID userId = getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user '" + userId + "' not found in database"));
    }

    /**
     * Check if the current user has the ADMIN role.
     */
    public boolean isAdmin() {
        return getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Check if the current user has the MANAGER role or above.
     */
    public boolean isManagerOrAbove() {
        return getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_MANAGER"));
    }

    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        return auth;
    }
}
