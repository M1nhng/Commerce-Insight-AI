package com.commerceinsight.auth.service;

import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * LoginAttemptService — persists failed-login bookkeeping in its OWN transaction.
 *
 * <p>{@link AuthService#login} runs inside {@code @Transactional} and rethrows
 * {@link org.springframework.security.authentication.BadCredentialsException} on a
 * wrong password. That rethrow rolls the surrounding transaction back — so an
 * increment / lockout written on the same transaction would be discarded and the
 * account would never actually lock. Writing it via {@code REQUIRES_NEW} here
 * commits the counter independently of the outer rollback.
 *
 * <p>Separate bean (not a private method in {@code AuthService}) so the
 * {@code REQUIRES_NEW} boundary is honoured — a self-invoked private method would
 * bypass the transactional proxy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** Lock the account once this many consecutive failures is reached. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;

    /**
     * Record one failed login for {@code userId}: increment the counter and,
     * on the threshold, set {@code locked = true}. Committed immediately,
     * regardless of the caller's transaction outcome.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        user.incrementFailedAttempts();
        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS && !user.isLocked()) {
            user.setLocked(true);
            log.warn("Account locked after {} failed attempts: id={}",
                    MAX_FAILED_ATTEMPTS, user.getId());
        }
        userRepository.save(user);
    }

    /**
     * Reset the failure counter after a successful login. Runs in its own
     * transaction for symmetry; safe to call inside the login transaction too.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedAttempts() != 0) {
                user.resetFailedAttempts();
                userRepository.save(user);
            }
        });
    }
}
