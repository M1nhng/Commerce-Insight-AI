package com.commerceinsight.export.service;

import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.dto.response.CustomerSummaryResponse;
import com.commerceinsight.customer.service.CustomerService;
import com.commerceinsight.export.config.ExportProperties;
import com.commerceinsight.export.dto.ReportColumn;
import com.commerceinsight.export.dto.ReportDocument;
import com.commerceinsight.export.dto.ReportTable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CustomerExportService — turns the existing customer read model into a
 * format-neutral {@link ReportDocument}.
 *
 * <p>Data is pulled exclusively through {@link CustomerService#findAll} (same
 * filters as {@code GET /api/v1/customers}). Only fields present on
 * {@link CustomerSummaryResponse} are exported; the list model contains no
 * authentication or credential data, and none is added here.
 */
@Service
@RequiredArgsConstructor
public class CustomerExportService {

    private final CustomerService customerService;
    private final ExportProperties exportProperties;

    private static final List<ReportColumn> COLUMNS = List.of(
            ReportColumn.text("Customer Code"),
            ReportColumn.text("First Name"),
            ReportColumn.text("Last Name"),
            ReportColumn.text("Full Name"),
            ReportColumn.text("Email"),
            ReportColumn.text("Phone"),
            ReportColumn.text("Status"),
            ReportColumn.text("Customer Group"),
            ReportColumn.dateTime("Created At (UTC)")
    );

    public ReportDocument buildReport(String keyword, CustomerStatus status, UUID groupId,
                                      Instant startDate, Instant endDate) {

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<CustomerSummaryResponse> customers = ExportQuerySupport.collectBounded(
                exportProperties.getMaxRows(),
                (pageIndex, pageSize) -> customerService.findAll(
                        keyword, status, groupId, startDate, endDate,
                        PageRequest.of(pageIndex, pageSize, sort)));

        List<List<Object>> rows = new ArrayList<>(customers.size());
        for (CustomerSummaryResponse c : customers) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(c.customerCode());
            row.add(c.firstName());
            row.add(c.lastName());
            row.add(c.fullName());
            row.add(c.email());
            row.add(c.phone());
            row.add(c.status() != null ? c.status().name() : null);
            row.add(c.groupName());
            row.add(c.createdAt());
            rows.add(row);
        }

        return ReportDocument.single("Customers Export", Instant.now(),
                new ReportTable("Customers", COLUMNS, rows));
    }
}
