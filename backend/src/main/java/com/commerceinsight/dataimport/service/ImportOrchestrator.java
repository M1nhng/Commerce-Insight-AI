package com.commerceinsight.dataimport.service;

import com.commerceinsight.dataimport.config.ImportProperties;
import com.commerceinsight.dataimport.domain.ImportFileType;
import com.commerceinsight.dataimport.domain.ImportJob;
import com.commerceinsight.dataimport.domain.ImportType;
import com.commerceinsight.dataimport.parser.CsvImportParser;
import com.commerceinsight.dataimport.parser.ExcelImportParser;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.FileValidator;
import com.commerceinsight.exception.ImportException;
import com.commerceinsight.security.SecurityContextHelper;
import com.commerceinsight.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * ImportOrchestrator — top-level coordinator for the entire import flow.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Validate file (FileValidator)</li>
 *   <li>Create ImportJob</li>
 *   <li>Parse rows (CSV or Excel)</li>
 *   <li>Dispatch to domain-specific import service</li>
 *   <li>Record per-row errors</li>
 *   <li>Finalize job with final status</li>
 * </ol>
 *
 * <p>Architecture Rule: This class coordinates — it must NOT contain business logic.
 * All domain validation and persistence goes through domain import services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportOrchestrator {

    private final FileValidator fileValidator;
    private final CsvImportParser csvParser;
    private final ExcelImportParser excelParser;
    private final ImportJobService jobService;
    private final ProductImportService productImportService;
    private final CustomerImportService customerImportService;
    private final OrderImportService orderImportService;
    private final ImportProperties importProperties;
    private final SecurityContextHelper securityContextHelper;

    /**
     * Runs the complete import pipeline for a given file and domain type.
     *
     * @param file       the uploaded multipart file
     * @param importType PRODUCT, CUSTOMER, or ORDER
     * @return the final ImportJob state after processing
     */
    public ImportJob run(MultipartFile file, ImportType importType) {
        // Step 1: Validate file format before creating a job
        ImportFileType fileType = fileValidator.validate(file);

        // Step 2: Create the job record
        User currentUser = securityContextHelper.getCurrentUserOrThrow();
        ImportJob job = jobService.createJob(
                file.getOriginalFilename(), fileType, importType, currentUser);
        UUID jobId = job.getId();

        log.info("Import job {} created: type={}, fileType={}, file={}",
                jobId, importType, fileType, file.getOriginalFilename());

        try {
            // Step 3: Parse the file
            jobService.markValidating(jobId);
            List<ParsedRow> rows = parseFile(file, fileType, importType);

            if (rows.isEmpty()) {
                jobService.markFailed(jobId, "File contains no data rows");
                return jobService.getOrThrow(jobId);
            }

            // Step 4: Dispatch to domain import service
            jobService.markImporting(jobId, rows.size());

            return switch (importType) {
                case PRODUCT  -> runProductImport(jobId, rows);
                case CUSTOMER -> runCustomerImport(jobId, rows);
                case ORDER    -> runOrderImport(jobId, rows);
            };

        } catch (ImportException e) {
            log.warn("Import job {} failed at file level: {}", jobId, e.getMessage());
            return jobService.markFailed(jobId, e.getMessage());
        } catch (Exception e) {
            log.error("Import job {} failed with unexpected error", jobId, e);
            return jobService.markFailed(jobId, "Unexpected error: " + e.getMessage());
        }
    }

    // ── Domain dispatchers ────────────────────────────────────────────────────

    private ImportJob runProductImport(UUID jobId, List<ParsedRow> rows) {
        for (ParsedRow row : rows) {
            RowImportResult result = productImportService.importRow(row);
            recordResult(jobId, result);
        }
        return jobService.finalize(jobId);
    }

    private ImportJob runCustomerImport(UUID jobId, List<ParsedRow> rows) {
        for (ParsedRow row : rows) {
            RowImportResult result = customerImportService.importRow(row);
            recordResult(jobId, result);
        }
        return jobService.finalize(jobId);
    }

    private ImportJob runOrderImport(UUID jobId, List<ParsedRow> rows) {
        // Group rows by orderNumber (preserving insertion order)
        Map<String, List<ParsedRow>> groups = new LinkedHashMap<>();
        for (ParsedRow row : rows) {
            String orderNumber = row.get("ordernumber");
            groups.computeIfAbsent(orderNumber, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<ParsedRow>> entry : groups.entrySet()) {
            RowImportResult result = orderImportService.importOrderGroup(entry.getKey(), entry.getValue());
            // For order groups: one success/failure per group (counted once per group)
            recordResult(jobId, result);
        }

        return jobService.finalize(jobId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<ParsedRow> parseFile(MultipartFile file, ImportFileType fileType, ImportType importType) {
        String[] expectedHeaders = switch (importType) {
            case PRODUCT  -> ProductImportService.REQUIRED_HEADERS;
            case CUSTOMER -> CustomerImportService.REQUIRED_HEADERS;
            case ORDER    -> OrderImportService.REQUIRED_HEADERS;
        };

        try (InputStream is = file.getInputStream()) {
            return switch (fileType) {
                case CSV  -> csvParser.parse(is, expectedHeaders, importProperties.getMaxRows());
                case XLSX -> excelParser.parse(is, expectedHeaders, importProperties.getMaxRows());
            };
        } catch (IOException e) {
            throw new ImportException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    private void recordResult(UUID jobId, RowImportResult result) {
        if (result.success()) {
            jobService.recordSuccess(jobId);
        } else {
            jobService.recordFailure(jobId);
            for (RowError error : result.errors()) {
                jobService.recordError(
                        jobId,
                        result.rowNumber(),
                        error.fieldName(),
                        error.rawValue(),
                        error.errorCode(),
                        error.errorMessage()
                );
            }
        }
    }
}
