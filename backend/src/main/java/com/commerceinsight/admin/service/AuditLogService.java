package com.commerceinsight.admin.service;

import com.commerceinsight.admin.domain.AuditLog;
import com.commerceinsight.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AuditLogService — writes immutable audit events to the audit_logs table.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>All writes are {@code @Async} — audit logging MUST NOT block the main request thread.</li>
 *   <li>Uses {@code Propagation.REQUIRES_NEW} — runs in its own transaction so that
 *       a rollback in the calling service does NOT prevent the audit event from being written.</li>
 *   <li>Failures are swallowed with a warning log — never propagate to the calling service.</li>
 * </ul>
 *
 * <p>Architecture Rule: Only this service writes to audit_logs.
 * No other service or controller may inject AuditLogRepository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ── Predefined Action Constants ──────────────────────────────────────

    public static final String ACTION_USER_LOGIN          = "USER_LOGIN";
    public static final String ACTION_USER_LOGIN_FAILED   = "USER_LOGIN_FAILED";
    public static final String ACTION_USER_LOGOUT         = "USER_LOGOUT";
    public static final String ACTION_USER_CREATED        = "USER_CREATED";
    public static final String ACTION_USER_UPDATED        = "USER_UPDATED";
    public static final String ACTION_USER_DELETED        = "USER_DELETED";
    public static final String ACTION_USER_ROLE_CHANGED   = "USER_ROLE_CHANGED";
    public static final String ACTION_USER_UNLOCKED       = "USER_UNLOCKED";
    public static final String ACTION_TOKEN_REFRESH       = "TOKEN_REFRESH";
    public static final String ACTION_REFRESH_TOKEN_REUSE = "REFRESH_TOKEN_REUSE_DETECTED";

    // ── Core Log Method ──────────────────────────────────────────────────

    /**
     * Asynchronously write an audit log entry.
     *
     * <p>This method is {@code @Async} — it runs on a background thread.
     * It uses {@code REQUIRES_NEW} to ensure the audit record is committed
     * independently of any ongoing caller transaction.
     *
     * @param userId     the UUID of the user who performed the action (null for system events)
     * @param action     the action type constant (e.g., {@link #ACTION_USER_LOGIN})
     * @param entityType the domain entity type (e.g., "User") — nullable
     * @param entityId   the UUID of the affected entity — nullable
     * @param oldValue   JSON string of previous state — nullable
     * @param newValue   JSON string of new state — nullable
     * @param ipAddress  the client IP address — nullable
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String action, String entityType, UUID entityId,
                    String oldValue, String newValue, String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(entry);
            log.debug("Audit log written: action={}, userId={}, entityType={}, entityId={}",
                    action, userId, entityType, entityId);

        } catch (Exception ex) {
            // Swallow — never let audit failure propagate to the caller
            log.warn("Failed to write audit log: action={}, userId={}, error={}",
                    action, userId, ex.getMessage());
        }
    }

    /**
     * Convenience overload — log a simple action without entity or state details.
     *
     * @param userId    the user performing the action
     * @param action    the action type constant
     * @param ipAddress the client IP address (nullable)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String action, String ipAddress) {
        log(userId, action, null, null, null, null, ipAddress);
    }
}
