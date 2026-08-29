package com.commerceinsight.dataimport.dto.response;

import com.commerceinsight.dataimport.domain.ImportFileType;
import com.commerceinsight.dataimport.domain.ImportJobStatus;
import com.commerceinsight.dataimport.domain.ImportType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * ImportJobSummaryResponse — condensed view for job list endpoint.
 */
@Builder
public record ImportJobSummaryResponse(

        UUID id,
        String fileName,
        ImportFileType fileType,
        ImportType importType,
        ImportJobStatus status,
        int totalRows,
        int successfulRows,
        int failedRows,
        Instant createdAt
) {}
