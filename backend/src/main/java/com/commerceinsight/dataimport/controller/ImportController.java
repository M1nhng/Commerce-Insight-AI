package com.commerceinsight.dataimport.controller;

import com.commerceinsight.dataimport.domain.ImportJob;
import com.commerceinsight.dataimport.domain.ImportJobStatus;
import com.commerceinsight.dataimport.domain.ImportType;
import com.commerceinsight.dataimport.dto.response.ImportErrorResponse;
import com.commerceinsight.dataimport.dto.response.ImportJobResponse;
import com.commerceinsight.dataimport.dto.response.ImportJobSummaryResponse;
import com.commerceinsight.dataimport.service.CustomerImportService;
import com.commerceinsight.dataimport.service.ImportJobService;
import com.commerceinsight.dataimport.service.ImportOrchestrator;
import com.commerceinsight.dataimport.service.OrderImportService;
import com.commerceinsight.dataimport.service.ProductImportService;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * ImportController — HTTP endpoints for data import operations.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/v1/import/products   — upload product CSV/XLSX (MANAGER, ADMIN)</li>
 *   <li>POST /api/v1/import/customers  — upload customer CSV/XLSX (MANAGER, ADMIN)</li>
 *   <li>POST /api/v1/import/orders     — upload order CSV/XLSX (MANAGER, ADMIN)</li>
 *   <li>GET  /api/v1/import/jobs       — paginated job history (STAFF, MANAGER, ADMIN)</li>
 *   <li>GET  /api/v1/import/jobs/{id}  — job detail (STAFF, MANAGER, ADMIN)</li>
 *   <li>GET  /api/v1/import/jobs/{id}/errors — paginated errors (STAFF, MANAGER, ADMIN)</li>
 *   <li>GET  /api/v1/import/templates/{type} — CSV template download (authenticated)</li>
 * </ul>
 *
 * <p>Architecture Rule: Controllers must never catch exceptions — propagate to GlobalExceptionHandler.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "CSV and Excel data import endpoints")
public class ImportController {

    private final ImportOrchestrator orchestrator;
    private final ImportJobService jobService;

    // ── Upload endpoints ──────────────────────────────────────────────────────

    @Operation(summary = "Import products from CSV or XLSX file")
    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportJobResponse>> importProducts(
            @RequestParam("file") MultipartFile file) {

        log.info("Product import requested: file={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportJob job = orchestrator.run(file, ImportType.PRODUCT);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(toResponse(job), "Product import completed"));
    }

    @Operation(summary = "Import customers from CSV or XLSX file")
    @PostMapping(value = "/customers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportJobResponse>> importCustomers(
            @RequestParam("file") MultipartFile file) {

        log.info("Customer import requested: file={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportJob job = orchestrator.run(file, ImportType.CUSTOMER);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(toResponse(job), "Customer import completed"));
    }

    @Operation(summary = "Import orders from CSV or XLSX file")
    @PostMapping(value = "/orders", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportJobResponse>> importOrders(
            @RequestParam("file") MultipartFile file) {

        log.info("Order import requested: file={}, size={}", file.getOriginalFilename(), file.getSize());
        ImportJob job = orchestrator.run(file, ImportType.ORDER);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(toResponse(job), "Order import completed"));
    }

    // ── Job history ───────────────────────────────────────────────────────────

    @Operation(summary = "List all import jobs (paginated)")
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ImportJobSummaryResponse>>> listJobs(
            @RequestParam(required = false) ImportType importType,
            @RequestParam(required = false) ImportJobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        PageResponse<ImportJobSummaryResponse> result = jobService.findAll(importType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Import jobs retrieved successfully"));
    }

    @Operation(summary = "Get import job details by ID")
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportJobResponse>> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                jobService.findById(id), "Import job retrieved successfully"));
    }

    @Operation(summary = "List errors for a specific import job (paginated)")
    @GetMapping("/jobs/{id}/errors")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ImportErrorResponse>>> getJobErrors(
            @PathVariable UUID id,
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String errorCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        PageResponse<ImportErrorResponse> errors = jobService.findErrors(id, fieldName, errorCode, pageable);
        return ResponseEntity.ok(ApiResponse.success(errors, "Import errors retrieved successfully"));
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    @Operation(summary = "Download a CSV template for the given import type")
    @GetMapping(value = "/templates/{type}", produces = "text/csv")
    public ResponseEntity<String> downloadTemplate(@PathVariable ImportType type) {
        String csv = switch (type) {
            case PRODUCT  -> String.join(",", ProductImportService.ALL_HEADERS);
            case CUSTOMER -> String.join(",", CustomerImportService.ALL_HEADERS);
            case ORDER    -> String.join(",", OrderImportService.ALL_HEADERS);
        };
        String filename = type.name().toLowerCase() + "_import_template.csv";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(csv + "\n");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ImportJobResponse toResponse(ImportJob job) {
        return jobService.findById(job.getId());
    }
}
