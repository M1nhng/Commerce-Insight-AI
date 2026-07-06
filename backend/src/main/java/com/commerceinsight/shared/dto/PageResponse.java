package com.commerceinsight.shared.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * PageResponse — paginated list response wrapped inside {@link ApiResponse}.
 *
 * <p>Wire format:
 * <pre>
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 10,
 *   "totalElements": 248,
 *   "totalPages": 25,
 *   "first": true,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> the type of elements in the page
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    private PageResponse(List<T> content, int page, int size,
                         long totalElements, int totalPages,
                         boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    /**
     * Construct a {@link PageResponse} directly from a Spring Data {@link Page}.
     *
     * @param page the Spring Data Page result
     * @param <T>  element type
     * @return a PageResponse wrapping the page data
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * Wrap a {@link Page} of entities already mapped to DTOs.
     * Use when content is a Page&lt;Entity&gt; that was already mapped via stream.
     *
     * @param content   mapped DTO list
     * @param sourcePage original Spring Data Page (for pagination metadata)
     * @param <T>        DTO type
     * @return a PageResponse wrapping the DTO list
     */
    public static <T> PageResponse<T> of(List<T> content, Page<?> sourcePage) {
        return new PageResponse<>(
                content,
                sourcePage.getNumber(),
                sourcePage.getSize(),
                sourcePage.getTotalElements(),
                sourcePage.getTotalPages(),
                sourcePage.isFirst(),
                sourcePage.isLast()
        );
    }
}
