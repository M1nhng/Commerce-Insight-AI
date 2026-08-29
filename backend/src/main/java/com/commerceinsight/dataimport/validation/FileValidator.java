package com.commerceinsight.dataimport.validation;

import com.commerceinsight.dataimport.domain.ImportFileType;
import com.commerceinsight.exception.ImportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.commerceinsight.dataimport.config.ImportProperties;

import java.util.Set;

/**
 * FileValidator — validates uploaded import files before any parsing occurs.
 *
 * <p>Checks:
 * <ul>
 *   <li>File is not null and not empty</li>
 *   <li>File size does not exceed configurable limit</li>
 *   <li>File extension is .csv or .xlsx (.xls and others are rejected)</li>
 *   <li>Content type (where provided by the client) matches the extension</li>
 * </ul>
 *
 * <p>Architecture Rule: validation must happen BEFORE creating an ImportJob
 * so we do not persist jobs for fundamentally invalid uploads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xlsx");
    private static final Set<String> CSV_MIME_TYPES = Set.of(
            "text/csv", "text/plain", "application/csv", "application/vnd.ms-excel");
    private static final Set<String> XLSX_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream");   // Some clients send this for .xlsx

    private final ImportProperties importProperties;

    /**
     * Validates the uploaded file and returns the detected {@link ImportFileType}.
     *
     * @param file the uploaded multipart file
     * @return CSV or XLSX based on the file extension
     * @throws ImportException if validation fails
     */
    public ImportFileType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImportException(
                    "[%s] No file was uploaded or the file is empty"
                            .formatted(ImportValidationCode.EMPTY_FILE));
        }

        // Size check
        long maxBytes = importProperties.maxFileSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new ImportException(
                    "[%s] File size %.2f MB exceeds the maximum allowed size of %d MB"
                            .formatted(ImportValidationCode.FILE_TOO_LARGE,
                                    file.getSize() / (1024.0 * 1024.0),
                                    importProperties.getMaxFileSizeMb()));
        }

        // Extension check
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new ImportException(
                    "[%s] File has no extension. Only .csv and .xlsx files are supported."
                            .formatted(ImportValidationCode.UNSUPPORTED_FILE_TYPE));
        }

        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase().trim();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ImportException(
                    "[%s] Unsupported file type '.%s'. Only .csv and .xlsx files are accepted."
                            .formatted(ImportValidationCode.UNSUPPORTED_FILE_TYPE, extension));
        }

        ImportFileType fileType = "csv".equals(extension) ? ImportFileType.CSV : ImportFileType.XLSX;

        // Content-type sanity check (only when content type is provided — some clients don't)
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !contentType.equals("application/octet-stream")) {
            validateContentType(contentType, fileType, extension);
        }

        log.debug("File validated: name={}, size={}, type={}", originalName, file.getSize(), fileType);
        return fileType;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateContentType(String contentType, ImportFileType fileType, String extension) {
        String normalizedType = contentType.toLowerCase().split(";")[0].trim();
        if (fileType == ImportFileType.CSV && !CSV_MIME_TYPES.contains(normalizedType)) {
            log.warn("Unexpected content-type '{}' for .csv file — accepting anyway", contentType);
        }
        if (fileType == ImportFileType.XLSX && !XLSX_MIME_TYPES.contains(normalizedType)) {
            // Log as warning; some browsers send wrong MIME for xlsx — we rely on extension
            log.warn("Unexpected content-type '{}' for .xlsx file — accepting based on extension", contentType);
        }
    }
}
