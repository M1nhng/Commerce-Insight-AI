package com.commerceinsight.auth.service;

import com.commerceinsight.auth.domain.LoginHistory;
import com.commerceinsight.auth.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * LoginHistoryService — writes an immutable row to {@code login_history} for
 * every login attempt.
 *
 * <p>Mirrors {@link com.commerceinsight.admin.service.AuditLogService}: writes
 * are {@code @Async} and run in a new transaction so a failure here never
 * affects the login flow. Failure reasons are short enum-like codes — no
 * password, no token, no stack trace is ever stored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    public static final String REASON_INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String REASON_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String REASON_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";

    private final LoginHistoryRepository loginHistoryRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String email, UUID userId, String ipAddress, String userAgent,
                       boolean success, String failureReason) {
        try {
            LoginHistory entry = LoginHistory.builder()
                    .email(truncate(email, 255))
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(truncate(userAgent, 2000))
                    .success(success)
                    .failureReason(failureReason)
                    .build();
            loginHistoryRepository.save(entry);
            log.debug("login_history written: success={}, reason={}, userId={}",
                    success, failureReason, userId);
        } catch (Exception ex) {
            log.warn("Failed to write login_history: success={}, reason={}, error={}",
                    success, failureReason, ex.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
