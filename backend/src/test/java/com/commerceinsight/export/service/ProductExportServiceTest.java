package com.commerceinsight.export.service;

import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.export.exception.ExportException;
import com.commerceinsight.product.dto.response.ProductSummaryResponse;
import com.commerceinsight.product.service.ProductService;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductExportService")
class ProductExportServiceTest {

    @Mock private ProductService productService;
    private ProductExportService service;

    private static final Instant CREATED = Instant.parse("2026-08-10T12:00:00Z");

    @BeforeEach
    void setUp() {
        ExportProperties props = new ExportProperties();
        props.setMaxRows(10_000);
        service = new ProductExportService(productService, props);
    }

    private ProductSummaryResponse product(String sku, String name, String category,
                                           BigDecimal price, boolean active) {
        return new ProductSummaryResponse(UUID.randomUUID(), sku, name, price,
                UUID.randomUUID(), category, 0, active, "http://img/" + sku, CREATED);
    }

    private PageResponse<ProductSummaryResponse> page(List<ProductSummaryResponse> content,
                                                      int pageIndex, int size, long total) {
        return PageResponse.from(new PageImpl<>(content, PageRequest.of(pageIndex, size), total));
    }

    @Test
    @DisplayName("maps the product summary read model into a single 'Products' table")
    void mapsRows() {
        when(productService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(
                        product("SKU-1", "Widget", "Tools", new BigDecimal("19.99"), true),
                        product("SKU-2", "Gadget", "Tools", new BigDecimal("5.00"), false)),
                        0, 1000, 2));

        ReportDocument doc = service.buildReport(null, null, null, null, null);

        assertThat(doc.title()).isEqualTo("Products Export");
        assertThat(doc.tables()).hasSize(1);
        ReportTable table = doc.tables().get(0);
        assertThat(table.name()).isEqualTo("Products");
        assertThat(table.columns()).extracting(c -> c.header())
                .containsExactly("SKU", "Name", "Category", "Price", "Active", "Image URL", "Created At (UTC)");
        assertThat(table.rows()).hasSize(2);
        assertThat(table.rows().get(0)).containsExactly(
                "SKU-1", "Widget", "Tools", new BigDecimal("19.99"), "Yes", "http://img/SKU-1", CREATED);
        assertThat(table.rows().get(1).get(4)).isEqualTo("No");
    }

    @Test
    @DisplayName("forwards every product filter to ProductService.findAll unchanged")
    void forwardsFilters() {
        when(productService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(), 0, 1000, 0));
        UUID categoryId = UUID.randomUUID();
        BigDecimal min = new BigDecimal("10");
        BigDecimal max = new BigDecimal("99");

        service.buildReport("wid", categoryId, Boolean.TRUE, min, max);

        verify(productService).findAll(eq("wid"), eq(categoryId), eq(Boolean.TRUE), eq(min), eq(max),
                any(Pageable.class));
    }

    @Test
    @DisplayName("reads in bounded batches and concatenates every page")
    void paginatesBeyondOneBatch() {
        List<ProductSummaryResponse> batch0 = IntStream.range(0, 1000)
                .mapToObj(i -> product("A" + i, "n", "c", BigDecimal.ONE, true)).toList();
        List<ProductSummaryResponse> batch1 = IntStream.range(0, 500)
                .mapToObj(i -> product("B" + i, "n", "c", BigDecimal.ONE, true)).toList();
        when(productService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(batch0, 0, 1000, 1500))
                .thenReturn(page(batch1, 1, 1000, 1500));

        ReportDocument doc = service.buildReport(null, null, null, null, null);

        assertThat(doc.tables().get(0).rows()).hasSize(1500);
        verify(productService, times(2)).findAll(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("rejects with EXPORT_ROW_LIMIT_EXCEEDED before loading a second page")
    void rowLimitExceeded() {
        ExportProperties tiny = new ExportProperties();
        tiny.setMaxRows(100);
        ProductExportService limited = new ProductExportService(productService, tiny);
        // page size == content size so PageImpl keeps the large totalElements we pass
        when(productService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(PageResponse.from(new PageImpl<>(
                        List.of(product("S", "n", "c", BigDecimal.ONE, true)), PageRequest.of(0, 1), 101)));

        assertThatThrownBy(() -> limited.buildReport(null, null, null, null, null))
                .isInstanceOf(ExportException.class)
                .satisfies(ex -> assertThat(((ExportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXPORT_ROW_LIMIT_EXCEEDED));

        verify(productService, times(1)).findAll(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("an empty result set yields a valid zero-row report")
    void emptyResult() {
        when(productService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(), 0, 1000, 0));

        ReportDocument doc = service.buildReport(null, null, null, null, null);

        assertThat(doc.tables().get(0).rows()).isEmpty();
        verify(productService, never()).findById(any());
    }

    @Test
    @DisplayName("requests newest-first ordering by createdAt")
    void sortsByCreatedAtDesc() {
        when(productService.findAll(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page(List.of(), 0, 1000, 0));

        service.buildReport(null, null, null, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(any(), any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }
}
