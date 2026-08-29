package com.commerceinsight.dataimport.repository;

import com.commerceinsight.dataimport.domain.ImportError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ImportErrorRepository — persistence for per-row import failure records.
 */
@Repository
public interface ImportErrorRepository extends JpaRepository<ImportError, UUID> {

    /** All errors for a job, ordered by row number. */
    Page<ImportError> findByImportJobIdOrderByRowNumber(UUID importJobId, Pageable pageable);

    /** Filter errors by field name within a job. */
    Page<ImportError> findByImportJobIdAndFieldNameOrderByRowNumber(
            UUID importJobId, String fieldName, Pageable pageable);

    /** Filter errors by error code within a job. */
    Page<ImportError> findByImportJobIdAndErrorCodeOrderByRowNumber(
            UUID importJobId, String errorCode, Pageable pageable);

    /** Count errors for a job (for validation). */
    long countByImportJobId(UUID importJobId);
}
