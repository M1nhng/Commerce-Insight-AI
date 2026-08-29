package com.commerceinsight.export.service;

import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.shared.dto.PageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * ExportQuerySupport — pulls a full result set out of an existing paginated
 * service method in bounded batches, without ever calling {@code findAll()}
 * unbounded.
 *
 * <p>The first page is fetched to learn {@code totalElements}; if that exceeds
 * the caller's {@code maxRows} cap the whole export is rejected up-front
 * (nothing further is loaded).
 */
final class ExportQuerySupport {

    /** Rows pulled per service call. */
    static final int BATCH_SIZE = 1000;

    private ExportQuerySupport() {}

    /** Fetches one page (0-based index) of at most {@code pageSize} rows. */
    @FunctionalInterface
    interface PageFetcher<T> {
        PageResponse<T> fetch(int pageIndex, int pageSize);
    }

    static <T> List<T> collectBounded(int maxRows, PageFetcher<T> fetcher) {
        PageResponse<T> first = fetcher.fetch(0, BATCH_SIZE);
        if (first.getTotalElements() > maxRows) {
            throw ExportException.rowLimitExceeded(maxRows);
        }

        List<T> all = new ArrayList<>(first.getContent());
        int totalPages = first.getTotalPages();
        for (int page = 1; page < totalPages; page++) {
            all.addAll(fetcher.fetch(page, BATCH_SIZE).getContent());
        }
        return all;
    }
}
