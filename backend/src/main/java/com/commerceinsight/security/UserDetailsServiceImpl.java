package com.commerceinsight.security;

import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UserDetailsServiceImpl — loads user details from the database for Spring Security.
 *
 * <p>The "username" used by Spring Security is the User's UUID string.
 * This ensures the JWT subject (sub claim = user UUID) maps to the correct UserDetails.
 *
 * <p>Architecture Rule: This class is part of the security infrastructure,
 * NOT the user business module. It only reads User data; it does not perform
 * business operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their UUID string (the JWT subject claim).
     *
     * @param userId the user's UUID as a string (must be a valid UUID)
     * @return fully populated UserDetails
     * @throws UsernameNotFoundException if no user exists with this UUID or UUID is malformed
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> {
                    log.debug("User not found for ID: {}", userId);
                    return new UsernameNotFoundException("User not found: " + userId);
                });

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(user.isLocked())
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}
