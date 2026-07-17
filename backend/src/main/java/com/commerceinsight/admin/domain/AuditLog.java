package com.commerceinsight.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * AuditLog — immutable record of a significant system event.
 *
 * <p>Maps to the {@code audit_logs} table. AuditLog records are NEVER
 * updated or soft-deleted. They are append-only by design.
 *
 * <p>Architecture Rule: AuditLog is written asynchronously via AuditLogService.
 * It must never block the main request thread.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** The user who performed the action. Nullable (e.g., for anonymous events). */
    @Column(name = "user_id")
    private UUID userId;

    /** The action type. e.g. USER_LOGIN, USER_ROLE_CHANGED. */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** The domain entity type affected. e.g. "User", "Product". Nullable. */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /** The ID of the affected entity. Nullable. */
    @Column(name = "entity_id")
    private UUID entityId;

    /** JSON snapshot of the entity state before the change. Nullable. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    /** JSON snapshot of the entity state after the change. Nullable. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    /** IP address of the requesting client. Nullable. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** User-Agent header from the request. Nullable. */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    /** When this audit event was recorded. Set on creation, never modified. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
