package com.commerceinsight.dataimport.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * ImportError — persists the details of a single row failure during import.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never store passwords, JWT tokens, API keys, or any sensitive data in rawValue.</li>
 *   <li>rawValue is sanitized and capped at 500 characters before persisting.</li>
 *   <li>Deleted when the parent ImportJob is deleted (CASCADE).</li>
 * </ul>
 *
 * <p>Maps to the {@code import_errors} table (created in V31).
 */
@Entity
@Table(name = "import_errors")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Parent import job. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    /** 1-based row number in the file (1 = first data row, after header). */
    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    /** Column/field that caused the error. Null for row-level errors. */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    /**
     * Raw cell value that caused the error. Sanitized — max 500 chars.
     * Never contains secrets, passwords, tokens, or full email addresses in sensitive contexts.
     */
    @Column(name = "raw_value", length = 500)
    private String rawValue;

    /** Stable error code for programmatic handling (e.g. MISSING_REQUIRED_FIELD). */
    @Column(name = "error_code", nullable = false, length = 100)
    private String errorCode;

    /** Human-readable error message for display. */
    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    /** When this error record was created. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
