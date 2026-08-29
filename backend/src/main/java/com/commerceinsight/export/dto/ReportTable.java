package com.commerceinsight.export.dto;

import java.util.List;

/**
 * ReportTable — a single tabular section of a report: an ordered list of
 * {@link ReportColumn}s and the data rows. Each row is a {@code List<Object>}
 * whose size and element order match {@link #columns()}.
 *
 * <p>An XLSX writer renders one worksheet per table; a PDF writer renders one
 * table block per table.
 *
 * @param name    sheet / section name (e.g. "Orders", "Revenue")
 * @param columns column definitions, in display order
 * @param rows    data rows; element {@code i} of each row corresponds to {@code columns.get(i)}
 */
public record ReportTable(String name, List<ReportColumn> columns, List<List<Object>> rows) {

    public ReportTable {
        columns = List.copyOf(columns);
        rows = List.copyOf(rows);
    }

    public int rowCount() {
        return rows.size();
    }
}
