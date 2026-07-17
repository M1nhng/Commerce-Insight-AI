package com.commerceinsight.admin.repository;

import com.commerceinsight.admin.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * AuditLogRepository — data access for {@link AuditLog}.
 *
 * <p>Architecture Rule: Only AuditLogService may inject this repository.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /** Paginated list of all audit logs, ordered by newest first. */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Filter audit logs by a specific action type. */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /** Filter audit logs by the user who performed the action. */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Filter audit logs by a specific entity (e.g., all events on a given user). */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, UUID entityId, Pageable pageable);
}
