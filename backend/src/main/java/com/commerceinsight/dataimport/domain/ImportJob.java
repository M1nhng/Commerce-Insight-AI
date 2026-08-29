package com.commerceinsight.dataimport.domain;

import com.commerceinsight.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * ImportJob — persistent record tracking the lifecycle of a CSV/Excel import operation.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Never expose this entity beyond the service layer. Use DTOs.</li>
 *   <li>Status transitions are controlled by ImportJobService only.</li>
 *   <li>Counter fields (totalRows, successfulRows, failedRows) are updated atomically
 *       via service methods — never directly from controllers.</li>
 * </ul>
 *
 * <p>Maps to the {@code import_jobs} table (created in V30).
 */
@Entity
@Table(name = "import_jobs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Original file name as uploaded. Stored for display only. */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /** Format of the uploaded file. */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private ImportFileType fileType;

    /** Which domain is being imported. */
    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", nullable = false, length = 50)
    private ImportType importType;

    /** Current lifecycle status of this job. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ImportJobStatus status = ImportJobStatus.UPLOADED;

    /** Total data rows parsed from the file (excluding header). */
    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private int totalRows = 0;

    /** Rows successfully imported into the domain. */
    @Column(name = "successful_rows", nullable = false)
    @Builder.Default
    private int successfulRows = 0;

    /** Rows that failed validation or business rules. */
    @Column(name = "failed_rows", nullable = false)
    @Builder.Default
    private int failedRows = 0;

    /** Timestamp when import processing began. Null until processing starts. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Timestamp when import completed (success, partial, or failure). */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** When the job record was created. Set by Spring Data Auditing. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * The user who triggered the import.
     * SET NULL if user is deleted (order history preservation).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ── Domain helpers ────────────────────────────────────────────────────────

    /** Increments successfulRows counter. */
    public void incrementSuccess() {
        this.successfulRows++;
    }

    /** Increments failedRows counter. */
    public void incrementFailed() {
        this.failedRows++;
    }
}
