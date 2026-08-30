package com.commerceinsight.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * LoginHistory — immutable record of a single login attempt (success or failure).
 *
 * <p>Maps the {@code login_history} table (Flyway {@code V12}). Append-only;
 * never updated or deleted. Written asynchronously via
 * {@link com.commerceinsight.auth.service.LoginHistoryService}.
 *
 * <p>Not a {@code BaseEntity}: the table has its own shape (no soft-delete, no
 * {@code updated_at}) and {@code user_id} is nullable so failed attempts on an
 * unknown email can still be recorded.
 */
@Entity
@Table(name = "login_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Null when the email did not match a user. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "success", nullable = false)
    private boolean success;

    /** e.g. INVALID_CREDENTIALS, ACCOUNT_LOCKED, ACCOUNT_DISABLED. Null on success. */
    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
