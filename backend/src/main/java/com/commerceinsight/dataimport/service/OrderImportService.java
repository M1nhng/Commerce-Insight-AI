package com.commerceinsight.dataimport.service;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.repository.CustomerRepository;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.order.domain.PaymentMethod;
import com.commerceinsight.order.dto.request.CreateOrderItemRequest;
import com.commerceinsight.order.dto.request.CreateOrderRequest;
import com.commerceinsight.order.service.OrderService;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * OrderImportService — validates and imports a group of order rows.
 *
 * <p>Order CSV format: one item per row; rows with the same {@code orderNumber} belong
 * to a single order. The entire group is imported as one atomic transaction.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>Calls {@link OrderService#createOrder(CreateOrderRequest)} — never bypasses it.</li>
 *   <li>Each order group runs in {@code PROPAGATION_REQUIRES_NEW} — one group failure
 *       never rolls back other groups.</li>
 *   <li>Customer resolved by email. Product resolved by SKU.</li>
 *   <li>Totals (subtotal, total) are NEVER imported — backend always recalculates.</li>
 * </ul>
 *
 * <p>Expected CSV headers (case-insensitive):
 * {@code ordernumber, customeremail, productsku, quantity, itemdiscount,
 *  paymentmethod, shippingfee, orderdiscount, tax, currency, notes}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderImportService {

    public static final String[] REQUIRED_HEADERS = {"ordernumber", "customeremail", "productsku", "quantity", "paymentmethod"};
    public static final String[] ALL_HEADERS = {
            "ordernumber", "customeremail", "productsku", "quantity", "itemdiscount",
            "paymentmethod", "shippingfee", "orderdiscount", "tax", "currency", "notes"
    };

    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    /**
     * Imports a single order group (all rows sharing one {@code orderNumber}) atomically.
     *
     * @param orderNumber the orderNumber identifying this group
     * @param rows        all ParsedRow objects belonging to this order
     * @return a RowImportResult scoped to the first row's row number
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowImportResult importOrderGroup(String orderNumber, List<ParsedRow> rows) {
        int firstRowNumber = rows.get(0).getRowNumber();

        // Validate all rows in the group first — fail-fast before any DB writes
        List<RowError> validationErrors = validateGroup(orderNumber, rows);
        if (!validationErrors.isEmpty()) {
            return RowImportResult.failure(firstRowNumber, validationErrors);
        }

        // The order-level fields come from the FIRST row of the group
        ParsedRow firstRow = rows.get(0);

        try {
            // Resolve customer by email
            String email = firstRow.get("customeremail");
            Customer customer = customerRepository.findByEmail(email)
                    .orElse(null);
            if (customer == null) {
                return RowImportResult.failure(firstRowNumber, List.of(
                        new RowError("customeremail", email,
                                ImportValidationCode.ENTITY_NOT_FOUND,
                                "Row %d: Customer with email '%s' not found"
                                        .formatted(firstRowNumber, email))));
            }
            if (customer.getStatus() != CustomerStatus.ACTIVE) {
                return RowImportResult.failure(firstRowNumber, List.of(
                        new RowError("customeremail", email,
                                ImportValidationCode.CUSTOMER_INACTIVE,
                                "Row %d: Customer '%s' is not ACTIVE".formatted(firstRowNumber, email))));
            }

            // Build order items (one per row)
            List<CreateOrderItemRequest> items = new ArrayList<>();
            for (ParsedRow row : rows) {
                String sku = row.get("productsku").toUpperCase();
                Product product = productRepository.findBySku(sku).orElse(null);
                if (product == null) {
                    return RowImportResult.failure(row.getRowNumber(), List.of(
                            new RowError("productsku", sku,
                                    ImportValidationCode.ENTITY_NOT_FOUND,
                                    "Row %d: Product with SKU '%s' not found".formatted(row.getRowNumber(), sku))));
                }
                if (!product.isActive()) {
                    return RowImportResult.failure(row.getRowNumber(), List.of(
                            new RowError("productsku", sku,
                                    ImportValidationCode.PRODUCT_INACTIVE,
                                    "Row %d: Product '%s' is inactive".formatted(row.getRowNumber(), sku))));
                }

                int qty = Integer.parseInt(row.get("quantity"));
                BigDecimal itemDiscount = row.hasValue("itemdiscount")
                        ? new BigDecimal(row.get("itemdiscount")) : BigDecimal.ZERO;
                items.add(new CreateOrderItemRequest(product.getId(), qty, itemDiscount));
            }

            // Parse order-level fields from first row
            PaymentMethod paymentMethod = PaymentMethod.valueOf(firstRow.get("paymentmethod").toUpperCase());
            BigDecimal shippingFee = firstRow.hasValue("shippingfee")
                    ? new BigDecimal(firstRow.get("shippingfee")) : BigDecimal.ZERO;
            BigDecimal orderDiscount = firstRow.hasValue("orderdiscount")
                    ? new BigDecimal(firstRow.get("orderdiscount")) : BigDecimal.ZERO;
            BigDecimal tax = firstRow.hasValue("tax")
                    ? new BigDecimal(firstRow.get("tax")) : BigDecimal.ZERO;
            String currency = firstRow.hasValue("currency") ? firstRow.get("currency") : "VND";
            String notes = firstRow.hasValue("notes") ? firstRow.get("notes") : null;

            CreateOrderRequest request = new CreateOrderRequest(
                    customer.getId(), items, null, null,
                    paymentMethod, shippingFee, orderDiscount, tax, currency, notes
            );

            orderService.createOrder(request);
            log.debug("Order imported from rows starting at {}: orderNumber={}, items={}",
                    firstRowNumber, orderNumber, items.size());
            return RowImportResult.success(firstRowNumber);

        } catch (BusinessRuleException e) {
            return RowImportResult.failure(firstRowNumber, List.of(
                    new RowError(null, null,
                            ImportValidationCode.BUSINESS_RULE_VIOLATION,
                            "Row %d: %s".formatted(firstRowNumber, e.getMessage()))));
        } catch (Exception e) {
            log.warn("Unexpected error importing order group '{}': {}", orderNumber, e.getMessage());
            return RowImportResult.failure(firstRowNumber, List.of(
                    new RowError(null, null,
                            ImportValidationCode.BUSINESS_RULE_VIOLATION,
                            "Row %d: %s".formatted(firstRowNumber, e.getMessage()))));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<RowError> validateGroup(String orderNumber, List<ParsedRow> rows) {
        List<RowError> errors = new ArrayList<>();

        for (ParsedRow row : rows) {
            // Required: orderNumber
            if (!row.hasValue("ordernumber")) {
                errors.add(new RowError("ordernumber", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                        "Row %d: 'orderNumber' is required".formatted(row.getRowNumber())));
            }

            // Required: customerEmail (only validate on first row — same for all rows in group)
            if (row.getRowNumber() == rows.get(0).getRowNumber() && !row.hasValue("customeremail")) {
                errors.add(new RowError("customeremail", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                        "Row %d: 'customerEmail' is required".formatted(row.getRowNumber())));
            }

            // Required: productSku
            if (!row.hasValue("productsku")) {
                errors.add(new RowError("productsku", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                        "Row %d: 'productSku' is required".formatted(row.getRowNumber())));
            }

            // Required: quantity
            if (!row.hasValue("quantity")) {
                errors.add(new RowError("quantity", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                        "Row %d: 'quantity' is required".formatted(row.getRowNumber())));
            } else {
                try {
                    int qty = Integer.parseInt(row.get("quantity"));
                    if (qty < 1) {
                        errors.add(new RowError("quantity", row.get("quantity"), ImportValidationCode.INVALID_VALUE,
                                "Row %d: 'quantity' must be at least 1".formatted(row.getRowNumber())));
                    }
                } catch (NumberFormatException e) {
                    errors.add(new RowError("quantity", row.get("quantity"), ImportValidationCode.INVALID_FORMAT,
                            "Row %d: 'quantity' is not a valid integer".formatted(row.getRowNumber())));
                }
            }

            // Required: paymentMethod (from first row only)
            if (row.getRowNumber() == rows.get(0).getRowNumber()) {
                if (!row.hasValue("paymentmethod")) {
                    errors.add(new RowError("paymentmethod", null, ImportValidationCode.MISSING_REQUIRED_FIELD,
                            "Row %d: 'paymentMethod' is required".formatted(row.getRowNumber())));
                } else {
                    try {
                        PaymentMethod.valueOf(row.get("paymentmethod").toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errors.add(new RowError("paymentmethod", row.get("paymentmethod"),
                                ImportValidationCode.INVALID_VALUE,
                                "Row %d: 'paymentMethod' must be one of: CASH, BANK_TRANSFER, CARD, OTHER"
                                        .formatted(row.getRowNumber())));
                    }
                }
            }

            // Optional: numeric fields
            validateOptionalDecimal(errors, row, "itemdiscount");
            validateOptionalDecimal(errors, row, "shippingfee");
            validateOptionalDecimal(errors, row, "orderdiscount");
            validateOptionalDecimal(errors, row, "tax");
        }

        return errors;
    }

    private void validateOptionalDecimal(List<RowError> errors, ParsedRow row, String field) {
        if (row.hasValue(field)) {
            try {
                BigDecimal val = new BigDecimal(row.get(field));
                if (val.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(new RowError(field, row.get(field), ImportValidationCode.NEGATIVE_VALUE,
                            "Row %d: '%s' must be >= 0".formatted(row.getRowNumber(), field)));
                }
            } catch (NumberFormatException e) {
                errors.add(new RowError(field, row.get(field), ImportValidationCode.INVALID_FORMAT,
                        "Row %d: '%s' is not a valid number".formatted(row.getRowNumber(), field)));
            }
        }
    }
}
