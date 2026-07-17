package com.commerceinsight.user.scheduler;

import com.commerceinsight.admin.service.AuditLogService;
import com.commerceinsight.user.domain.User;
import com.commerceinsight.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * AccountUnlockScheduler — automatically unlocks accounts after 15 minutes.
 *
 * <p>Per the security spec (docs/06_AUTHENTICATION.md):
 * "Account lock after 5 failed login attempts for 15 minutes"
 *
 * <p>This scheduler runs every 5 minutes and finds any locked users whose
 * account was locked more than 15 minutes ago, then unlocks them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountUnlockScheduler {

    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * Auto-unlock accounts locked more than 15 minutes ago.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes in milliseconds
    @Transactional
    public void unlockExpiredLocks() {
        Instant threshold = Instant.now().minus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);
        List<User> lockedUsers = userRepository.findLockedUsersLockedBefore(threshold);

        if (lockedUsers.isEmpty()) {
            return;
        }

        log.info("Auto-unlock scheduler: found {} account(s) to unlock", lockedUsers.size());

        for (User user : lockedUsers) {
            user.setLocked(false);
            user.resetFailedAttempts();
            userRepository.save(user);

            log.info("Account auto-unlocked after {} minutes: {} ({})",
                    LOCK_DURATION_MINUTES, user.getEmail(), user.getId());

            // Write audit entry (system event — no adminId)
            auditLogService.log(null, AuditLogService.ACTION_USER_UNLOCKED, "User",
                    user.getId(), null,
                    "{\"reason\":\"auto-unlock after 15 minutes\"}", null);
        }
    }
}
