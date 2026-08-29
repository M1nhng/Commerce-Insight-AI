package com.commerceinsight.dataimport.dto.response;

import com.commerceinsight.dataimport.domain.ImportFileType;
import com.commerceinsight.dataimport.domain.ImportJobStatus;
import com.commerceinsight.dataimport.domain.ImportType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * ImportJobResponse — full details of an import job.
 * Returned on upload (201) and on GET /import/jobs/{id}.
 */
@Builder
public record ImportJobResponse(

        UUID id,
        String fileName,
        ImportFileType fileType,
        ImportType importType,
        ImportJobStatus status,
        int totalRows,
        int successfulRows,
        int failedRows,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        String createdByEmail
) {}
