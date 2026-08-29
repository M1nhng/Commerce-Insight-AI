package com.commerceinsight.export.dto;

/**
 * ExportFile — the finished export payload handed back to the controller:
 * the raw bytes plus everything needed to build the HTTP download response.
 *
 * @param filename     download filename, e.g. {@code products_2026-08-29.xlsx}
 * @param contentType  MIME type for the {@code Content-Type} header
 * @param content      the file bytes (XLSX or PDF)
 */
public record ExportFile(String filename, String contentType, byte[] content) {

    public long size() {
        return content == null ? 0 : content.length;
    }
}
