package com.commerceinsight.dataimport.service;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.category.repository.CategoryRepository;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.product.dto.request.CreateProductRequest;
import com.commerceinsight.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ProductImportService — validates and imports a single product row.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Calls {@link ProductService#create(CreateProductRequest)} — never bypasses it.</li>
 *   <li>Each row runs in {@code PROPAGATION_REQUIRES_NEW} so one failure never
 *       rolls back a previously committed row.</li>
 *   <li>Category is resolved by name (case-insensitive). Null if not provided.</li>
 * </ul>
 *
 * <p>Expected CSV headers (case-insensitive):
 * {@code sku, name, description, price, costprice, categoryname, imageurl}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImportService {

    /** Column headers expected in the product import file. */
    public static final String[] REQUIRED_HEADERS = {"sku", "name", "price"};

    /** All columns (optional ones included — used for documentation/template). */
    public static final String[] ALL_HEADERS =
            {"sku", "name", "description", "price", "costprice", "categoryname", "imageurl"};

    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    /**
     * Imports a single product row.
     *
     * @return a {@link RowImportResult} — success or failure with error details
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowImportResult importRow(ParsedRow row) {
        List<RowError> errors = validateRow(row);
        if (!errors.isEmpty()) {
            return RowImportResult.failure(row.getRowNumber(), errors);
        }

        try {
            String categoryName = row.get("categoryname");
            UUID categoryId = null;
            if (!categoryName.isEmpty()) {
                Category category = categoryRepository.findByNameIgnoreCase(categoryName)
                        .orElse(null);
                if (category != null) {
                    categoryId = category.getId();
                } else {
                    return RowImportResult.failure(row.getRowNumber(), List.of(
                            new RowError("categoryname", categoryName,
                                    ImportValidationCode.ENTITY_NOT_FOUND,
                                    "Category '%s' not found".formatted(categoryName))));
                }
            }

            CreateProductRequest request = new CreateProductRequest(
                    row.get("sku"),
                    row.get("name"),
                    row.hasValue("description") ? row.get("description") : null,
                    new BigDecimal(row.get("price")),
                    row.hasValue("costprice") ? new BigDecimal(row.get("costprice")) : null,
                    row.hasValue("imageurl")  ? row.get("imageurl")  : null,
                    categoryId,
                    0   // No initial stock seeded via import
            );

            productService.create(request);
            log.debug("Product imported from row {}: sku={}", row.getRowNumber(), row.get("sku"));
            return RowImportResult.success(row.getRowNumber());

        } catch (DuplicateResourceException e) {
            return RowImportResult.failure(row.getRowNumber(), List.of(
                    new RowError("sku", row.get("sku"),
                            ImportValidationCode.DUPLICATE_RECORD,
                            "Product with SKU '%s' already exists".formatted(row.get("sku")))));
        } catch (Exception e) {
            log.warn("Unexpected error importing product row {}: {}", row.getRowNumber(), e.getMessage());
            return RowImportResult.failure(row.getRowNumber(), List.of(
                    new RowError(null, null,
                            ImportValidationCode.BUSINESS_RULE_VIOLATION,
                            "Row %d: %s".formatted(row.getRowNumber(), e.getMessage()))));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<RowError> validateRow(ParsedRow row) {
        List<RowError> errors = new ArrayList<>();

        // Required: sku
        if (!row.hasValue("sku")) {
            errors.add(new RowError("sku", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                    "Row %d: 'sku' is required".formatted(row.getRowNumber())));
        } else if (row.get("sku").length() > 100) {
            errors.add(new RowError("sku", row.get("sku"), ImportValidationCode.VALUE_TOO_LONG,
                    "Row %d: 'sku' must not exceed 100 characters".formatted(row.getRowNumber())));
        }

        // Required: name
        if (!row.hasValue("name")) {
            errors.add(new RowError("name", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                    "Row %d: 'name' is required".formatted(row.getRowNumber())));
        } else if (row.get("name").length() > 255) {
            errors.add(new RowError("name", row.get("name"), ImportValidationCode.VALUE_TOO_LONG,
                    "Row %d: 'name' must not exceed 255 characters".formatted(row.getRowNumber())));
        }

        // Required: price
        if (!row.hasValue("price")) {
            errors.add(new RowError("price", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                    "Row %d: 'price' is required".formatted(row.getRowNumber())));
        } else {
            try {
                BigDecimal price = new BigDecimal(row.get("price"));
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(new RowError("price", row.get("price"), ImportValidationCode.NEGATIVE_VALUE,
                            "Row %d: 'price' must be >= 0".formatted(row.getRowNumber())));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowError("price", row.get("price"), ImportValidationCode.INVALID_FORMAT,
                        "Row %d: 'price' is not a valid number".formatted(row.getRowNumber())));
            }
        }

        // Optional: costprice
        if (row.hasValue("costprice")) {
            try {
                BigDecimal cost = new BigDecimal(row.get("costprice"));
                if (cost.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(new RowError("costprice", row.get("costprice"), ImportValidationCode.NEGATIVE_VALUE,
                            "Row %d: 'costprice' must be >= 0".formatted(row.getRowNumber())));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowError("costprice", row.get("costprice"), ImportValidationCode.INVALID_FORMAT,
                        "Row %d: 'costprice' is not a valid number".formatted(row.getRowNumber())));
            }
        }

        return errors;
    }
}
