package com.commerceinsight.export.dto;

import java.time.Instant;
import java.util.List;

/**
 * ReportDocument — the format-neutral representation of a report, produced by the
 * {@code *ExportService} classes and consumed by the Excel / PDF writers.
 *
 * <p>It deliberately contains no domain logic: services fill it from existing
 * read models, writers only render it.
 *
 * @param title       human-readable report title, shown at the top of the file
 * @param generatedAt generation timestamp, shown under the title
 * @param tables      one or more {@link ReportTable} sections
 */
public record ReportDocument(String title, Instant generatedAt, List<ReportTable> tables) {

    public ReportDocument {
        tables = List.copyOf(tables);
    }

    public static ReportDocument single(String title, ReportTable table) {
        return new ReportDocument(title, Instant.now(), List.of(table));
    }

    public static ReportDocument single(String title, Instant generatedAt, ReportTable table) {
        return new ReportDocument(title, generatedAt, List.of(table));
    }

    /** Total data rows across every table — used for the row-limit guard. */
    public int totalRows() {
        return tables.stream().mapToInt(ReportTable::rowCount).sum();
    }
}
