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
     * @param userId the user's UUID as a string
     * @return fully populated UserDetails
     * @throws UsernameNotFoundException if no user exists with this UUID
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByIdString(userId)
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
