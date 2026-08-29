package com.commerceinsight.export.dto;

import com.commerceinsight.export.exception.ExportException;

/**
 * ExportFormat — supported output formats for report exports.
 *
 * <p>Parsing is case-insensitive (consistent with the lenient enum handling used
 * elsewhere, e.g. {@code AnalyticsService} normalises {@code groupBy}). An
 * unrecognised value is never silently defaulted — it raises
 * {@link ExportException#invalidFormat(String)} which maps to HTTP 400.
 */
public enum ExportFormat {

    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    PDF("application/pdf", "pdf");

    private final String contentType;
    private final String fileExtension;

    ExportFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String contentType() {
        return contentType;
    }

    public String fileExtension() {
        return fileExtension;
    }

    /**
     * Parse a request parameter into an {@link ExportFormat}, case-insensitively.
     *
     * @param raw the raw {@code format} query parameter
     * @return the matching format
     * @throws ExportException (HTTP 400) if {@code raw} is null, blank, or unrecognised
     */
    public static ExportFormat from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ExportException.invalidFormat(raw);
        }
        String normalised = raw.trim().toUpperCase();
        for (ExportFormat format : values()) {
            if (format.name().equals(normalised)) {
                return format;
            }
        }
        throw ExportException.invalidFormat(raw);
    }
}
