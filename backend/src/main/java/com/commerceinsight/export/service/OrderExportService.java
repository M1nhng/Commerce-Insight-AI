package com.commerceinsight.export.service;

import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import com.commerceinsight.order.domain.OrderStatus;
import com.commerceinsight.order.domain.PaymentStatus;
import com.commerceinsight.order.dto.response.OrderSummaryResponse;
import com.commerceinsight.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * OrderExportService — turns the existing order read model into a format-neutral
 * {@link ReportDocument}.
 *
 * <p>Data is pulled exclusively through {@link OrderService#findAll} (same
 * filters as {@code GET /api/v1/orders}). The report is strictly
 * <b>one row per order</b> — orders with multiple items are never duplicated;
 * the item count is shown as a single column.
 *
 * <p>Financial fields are taken verbatim from {@link OrderSummaryResponse#total()}
 * — no total is recomputed here. The list model does not expose subtotal /
 * discount / shipping / tax / completedAt, so those are omitted rather than
 * fetched per-order.
 */
@Service
@RequiredArgsConstructor
public class OrderExportService {

    private final OrderService orderService;
    private final ExportProperties exportProperties;

    private static final List<ReportColumn> COLUMNS = List.of(
            ReportColumn.text("Order Number"),
            ReportColumn.text("Customer"),
            ReportColumn.text("Status"),
            ReportColumn.text("Payment Status"),
            ReportColumn.text("Currency"),
            ReportColumn.integer("Items"),
            ReportColumn.money("Total"),
            ReportColumn.dateTime("Created At (UTC)"),
            ReportColumn.dateTime("Updated At (UTC)")
    );

    public ReportDocument buildReport(String keyword, UUID customerId, OrderStatus status,
                                      PaymentStatus paymentStatus, Instant dateFrom, Instant dateTo) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<OrderSummaryResponse> orders = ExportQuerySupport.collectBounded(
                exportProperties.getMaxRows(),
                (pageIndex, pageSize) -> orderService.findAll(
                        keyword, customerId, status, paymentStatus, dateFrom, dateTo,
                        PageRequest.of(pageIndex, pageSize, sort)));

        List<List<Object>> rows = new ArrayList<>(orders.size());
        for (OrderSummaryResponse o : orders) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(o.orderNumber());
            row.add(o.customerName());
            row.add(o.status() != null ? o.status().name() : null);
            row.add(o.paymentStatus() != null ? o.paymentStatus().name() : null);
            row.add(o.currency());
            row.add(o.itemCount());
            row.add(o.total());
            row.add(o.createdAt());
            row.add(o.updatedAt());
            rows.add(row);
        }

        return ReportDocument.single("Orders Export", Instant.now(),
                new ReportTable("Orders", COLUMNS, rows));
    }
}
