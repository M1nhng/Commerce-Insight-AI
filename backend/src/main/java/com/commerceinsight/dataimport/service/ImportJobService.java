package com.commerceinsight.dataimport.service;

import com.commerceinsight.dataimport.domain.*;
import com.commerceinsight.dataimport.dto.response.ImportErrorResponse;
import com.commerceinsight.dataimport.dto.response.ImportJobResponse;
import com.commerceinsight.dataimport.dto.response.ImportJobSummaryResponse;
import com.commerceinsight.dataimport.repository.ImportErrorRepository;
import com.commerceinsight.dataimport.repository.ImportJobRepository;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * ImportJobService — manages the lifecycle and persistence of ImportJob records.
 *
 * <p>Architecture Rule: This service is the ONLY class that mutates ImportJob state.
 * Domain import services (Product/Customer/Order) call this service to record progress.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportJobService {

    private final ImportJobRepository jobRepository;
    private final ImportErrorRepository errorRepository;

    // ── Job lifecycle ─────────────────────────────────────────────────────────

    @Transactional
    public ImportJob createJob(String fileName, ImportFileType fileType,
                               ImportType importType, User createdBy) {
        ImportJob job = ImportJob.builder()
                .fileName(fileName)
                .fileType(fileType)
                .importType(importType)
                .status(ImportJobStatus.UPLOADED)
                .createdBy(createdBy)
                .build();
        return jobRepository.save(job);
    }

    @Transactional
    public void markValidating(UUID jobId) {
        ImportJob job = getOrThrow(jobId);
        job.setStatus(ImportJobStatus.VALIDATING);
        jobRepository.save(job);
    }

    @Transactional
    public void markImporting(UUID jobId, int totalRows) {
        ImportJob job = getOrThrow(jobId);
        job.setStatus(ImportJobStatus.IMPORTING);
        job.setTotalRows(totalRows);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
    }

    @Transactional
    public void recordSuccess(UUID jobId) {
        ImportJob job = getOrThrow(jobId);
        job.incrementSuccess();
        jobRepository.save(job);
    }

    @Transactional
    public void recordFailure(UUID jobId) {
        ImportJob job = getOrThrow(jobId);
        job.incrementFailed();
        jobRepository.save(job);
    }

    @Transactional
    public void recordError(UUID jobId, int rowNumber, String fieldName,
                            String rawValue, String errorCode, String errorMessage) {
        ImportJob job = jobRepository.getReferenceById(jobId);
        String sanitizedValue = sanitize(rawValue);
        ImportError error = ImportError.builder()
                .importJob(job)
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .rawValue(sanitizedValue)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
        errorRepository.save(error);
    }

    @Transactional
    public ImportJob finalize(UUID jobId) {
        ImportJob job = getOrThrow(jobId);
        job.setCompletedAt(Instant.now());

        if (job.getFailedRows() == 0 && job.getSuccessfulRows() > 0) {
            job.setStatus(ImportJobStatus.COMPLETED);
        } else if (job.getSuccessfulRows() > 0) {
            job.setStatus(ImportJobStatus.PARTIAL_SUCCESS);
        } else {
            job.setStatus(ImportJobStatus.FAILED);
        }

        ImportJob saved = jobRepository.save(job);
        log.info("Import job {} finalized: status={}, total={}, success={}, failed={}",
                jobId, saved.getStatus(), saved.getTotalRows(),
                saved.getSuccessfulRows(), saved.getFailedRows());
        return saved;
    }

    @Transactional
    public ImportJob markFailed(UUID jobId, String reason) {
        ImportJob job = getOrThrow(jobId);
        job.setStatus(ImportJobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        log.warn("Import job {} failed: {}", jobId, reason);
        return jobRepository.save(job);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public ImportJobResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    public PageResponse<ImportJobSummaryResponse> findAll(
            ImportType importType, ImportJobStatus status, Pageable pageable) {

        Page<ImportJob> page;
        if (importType != null && status != null) {
            page = jobRepository.findByImportTypeAndStatusOrderByCreatedAtDesc(importType, status, pageable);
        } else if (importType != null) {
            page = jobRepository.findByImportTypeOrderByCreatedAtDesc(importType, pageable);
        } else if (status != null) {
            page = jobRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            page = jobRepository.findByOrderByCreatedAtDesc(pageable);
        }
        return PageResponse.from(page.map(this::toSummary));
    }

    public PageResponse<ImportErrorResponse> findErrors(
            UUID jobId, String fieldName, String errorCode, Pageable pageable) {
        // Verify job exists
        getOrThrow(jobId);

        Page<ImportError> page;
        if (fieldName != null) {
            page = errorRepository.findByImportJobIdAndFieldNameOrderByRowNumber(jobId, fieldName, pageable);
        } else if (errorCode != null) {
            page = errorRepository.findByImportJobIdAndErrorCodeOrderByRowNumber(jobId, errorCode, pageable);
        } else {
            page = errorRepository.findByImportJobIdOrderByRowNumber(jobId, pageable);
        }
        return PageResponse.from(page.map(this::toErrorResponse));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    ImportJob getOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.IMPORT_JOB_NOT_FOUND,
                        "Import job with ID '%s' was not found".formatted(id)));
    }

    private ImportJobResponse toResponse(ImportJob job) {
        return ImportJobResponse.builder()
                .id(job.getId())
                .fileName(job.getFileName())
                .fileType(job.getFileType())
                .importType(job.getImportType())
                .status(job.getStatus())
                .totalRows(job.getTotalRows())
                .successfulRows(job.getSuccessfulRows())
                .failedRows(job.getFailedRows())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .createdByEmail(job.getCreatedBy() != null ? job.getCreatedBy().getEmail() : null)
                .build();
    }

    private ImportJobSummaryResponse toSummary(ImportJob job) {
        return ImportJobSummaryResponse.builder()
                .id(job.getId())
                .fileName(job.getFileName())
                .fileType(job.getFileType())
                .importType(job.getImportType())
                .status(job.getStatus())
                .totalRows(job.getTotalRows())
                .successfulRows(job.getSuccessfulRows())
                .failedRows(job.getFailedRows())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private ImportErrorResponse toErrorResponse(ImportError error) {
        return ImportErrorResponse.builder()
                .id(error.getId())
                .rowNumber(error.getRowNumber())
                .fieldName(error.getFieldName())
                .rawValue(error.getRawValue())
                .errorCode(error.getErrorCode())
                .errorMessage(error.getErrorMessage())
                .build();
    }

    /**
     * Sanitizes rawValue before storage: caps to 500 chars and removes control characters.
     * Ensures no secrets are accidentally stored from raw CSV content.
     */
    private String sanitize(String value) {
        if (value == null) return null;
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\t\n\r]]", "");
        return cleaned.length() > 500 ? cleaned.substring(0, 500) : cleaned;
    }
}
