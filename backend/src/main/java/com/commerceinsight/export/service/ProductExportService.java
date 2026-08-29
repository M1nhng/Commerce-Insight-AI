package com.commerceinsight.export.service;

import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.product.dto.response.ProductSummaryResponse;
import com.commerceinsight.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ProductExportService — turns the existing product read model into a
 * format-neutral {@link ReportDocument}.
 *
 * <p>Data is pulled exclusively through {@link ProductService#findAll} (same
 * filters as {@code GET /api/v1/products}); no repository, no business logic.
 *
 * <p>The list read model ({@link ProductSummaryResponse}) does not carry
 * description, cost price, updated-at or real stock levels — those are omitted
 * rather than fetched per-row (which would be an N+1 across the product /
 * inventory boundary).
 */
@Service
@RequiredArgsConstructor
public class ProductExportService {

    private final ProductService productService;
    private final ExportProperties exportProperties;

    private static final List<ReportColumn> COLUMNS = List.of(
            ReportColumn.text("SKU"),
            ReportColumn.text("Name"),
            ReportColumn.text("Category"),
            ReportColumn.money("Price"),
            ReportColumn.text("Active"),
            ReportColumn.text("Image URL"),
            ReportColumn.dateTime("Created At (UTC)")
    );

    public ReportDocument buildReport(String search, UUID categoryId, Boolean active,
                                      BigDecimal priceMin, BigDecimal priceMax) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<ProductSummaryResponse> products = ExportQuerySupport.collectBounded(
                exportProperties.getMaxRows(),
                (pageIndex, pageSize) -> productService.findAll(
                        search, categoryId, active, priceMin, priceMax,
                        PageRequest.of(pageIndex, pageSize, sort)));

        List<List<Object>> rows = new ArrayList<>(products.size());
        for (ProductSummaryResponse p : products) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(p.sku());
            row.add(p.name());
            row.add(p.categoryName());
            row.add(p.price());
            row.add(p.active() ? "Yes" : "No");
            row.add(p.imageUrl());
            row.add(p.createdAt());
            rows.add(row);
        }

        return ReportDocument.single("Products Export", Instant.now(),
                new ReportTable("Products", COLUMNS, rows));
    }
}
