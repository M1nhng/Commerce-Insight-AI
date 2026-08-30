package com.commerceinsight.auth;

import com.commerceinsight.auth.service.LoginAttemptService;
import com.commerceinsight.user.domain.Role;
import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link LoginAttemptService} — the failed-attempt counter /
 * lockout logic moved here in Sprint 13A so it commits in its own transaction
 * and is not rolled back by the {@code BadCredentialsException} rethrow in
 * {@code AuthService.login}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private LoginAttemptService service;

    private User user;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("attempt@example.com")
                .passwordHash("hash")
                .firstName("A").lastName("B")
                .role(Role.STAFF)
                .build();
        user.setId(userId);
    }

    @Test
    @DisplayName("recordFailure increments the counter and persists")
    void recordFailure_increments() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.recordFailure(userId);

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.isLocked()).isFalse();
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("recordFailure locks the account on the 5th consecutive failure")
    void recordFailure_locksAtThreshold() {
        user.setFailedAttempts(4);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.recordFailure(userId);

        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.isLocked()).isTrue();
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("recordFailure is a no-op when the user no longer exists")
    void recordFailure_userGone() {
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        service.recordFailure(userId);

        then(userRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("recordSuccess clears a non-zero counter")
    void recordSuccess_resets() {
        user.setFailedAttempts(3);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.recordSuccess(userId);

        assertThat(user.getFailedAttempts()).isZero();
        then(userRepository).should().save(user);
    }

    @Test
    @DisplayName("recordSuccess does not write when the counter is already zero")
    void recordSuccess_noWriteWhenZero() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        service.recordSuccess(userId);

        then(userRepository).should(never()).save(any());
    }
}
