package com.commerceinsight.dataimport.repository;

import com.commerceinsight.dataimport.domain.ImportJob;
import com.commerceinsight.dataimport.domain.ImportJobStatus;
import com.commerceinsight.dataimport.domain.ImportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * ImportJobRepository — persistence for import job lifecycle tracking.
 */
@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    Page<ImportJob> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<ImportJob> findByImportTypeOrderByCreatedAtDesc(ImportType importType, Pageable pageable);

    Page<ImportJob> findByStatusOrderByCreatedAtDesc(ImportJobStatus status, Pageable pageable);

    Page<ImportJob> findByImportTypeAndStatusOrderByCreatedAtDesc(
            ImportType importType, ImportJobStatus status, Pageable pageable);
}
