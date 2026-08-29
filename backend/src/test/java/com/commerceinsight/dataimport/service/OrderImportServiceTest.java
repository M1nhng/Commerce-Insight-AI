package com.commerceinsight.dataimport.service;

import com.commerceinsight.customer.domain.Customer;
import com.commerceinsight.customer.domain.CustomerStatus;
import com.commerceinsight.customer.repository.CustomerRepository;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.order.service.OrderService;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.repository.ProductRepository;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * OrderImportServiceTest — unit tests for {@link OrderImportService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderImportService Unit Tests")
class OrderImportServiceTest {

    @Mock private OrderService orderService;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private OrderImportService service;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid single-item order group — calls OrderService.createOrder, returns success")
    void validSingleItemGroup_returnsSuccess() {
        Customer customer = mockActiveCustomer();
        Product product = mockActiveProduct("SKU-001");

        given(customerRepository.findByEmail("john@example.com")).willReturn(Optional.of(customer));
        given(productRepository.findBySku("SKU-001")).willReturn(Optional.of(product));
        given(orderService.createOrder(any())).willReturn(null);

        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001",
                "customeremail", "john@example.com",
                "productsku", "SKU-001",
                "quantity", "2",
                "paymentmethod", "CASH"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isTrue();
        then(orderService).should().createOrder(any());
    }

    @Test
    @DisplayName("Multi-item order group — all items bundled into one order")
    void multiItemGroup_bundledIntoOneOrder() {
        Customer customer = mockActiveCustomer();
        Product p1 = mockActiveProduct("SKU-001");
        Product p2 = mockActiveProduct("SKU-002");

        given(customerRepository.findByEmail("john@example.com")).willReturn(Optional.of(customer));
        given(productRepository.findBySku("SKU-001")).willReturn(Optional.of(p1));
        given(productRepository.findBySku("SKU-002")).willReturn(Optional.of(p2));
        given(orderService.createOrder(any())).willReturn(null);

        List<ParsedRow> rows = List.of(
                row(1, Map.of("ordernumber", "ORD-001", "customeremail", "john@example.com",
                        "productsku", "SKU-001", "quantity", "1", "paymentmethod", "CASH")),
                row(2, Map.of("ordernumber", "ORD-001", "customeremail", "john@example.com",
                        "productsku", "SKU-002", "quantity", "3", "paymentmethod", "CASH"))
        );

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isTrue();
        then(orderService).should(times(1)).createOrder(any());
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Test
    @DisplayName("Customer not found — returns failure with ENTITY_NOT_FOUND")
    void customerNotFound_returnsFailure() {
        given(customerRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());

        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001",
                "customeremail", "ghost@example.com",
                "productsku", "SKU-001",
                "quantity", "1",
                "paymentmethod", "CASH"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e ->
                ImportValidationCode.ENTITY_NOT_FOUND.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Customer BLOCKED — returns failure with CUSTOMER_INACTIVE")
    void blockedCustomer_returnsFailure() {
        Customer customer = mockActiveCustomer();
        customer.setStatus(CustomerStatus.BLOCKED);
        given(customerRepository.findByEmail("john@example.com")).willReturn(Optional.of(customer));

        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001", "customeremail", "john@example.com",
                "productsku", "SKU-001", "quantity", "1", "paymentmethod", "CASH"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e ->
                ImportValidationCode.CUSTOMER_INACTIVE.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Product SKU not found — returns failure with ENTITY_NOT_FOUND")
    void productNotFound_returnsFailure() {
        Customer customer = mockActiveCustomer();
        given(customerRepository.findByEmail("john@example.com")).willReturn(Optional.of(customer));
        given(productRepository.findBySku("MISSING-SKU")).willReturn(Optional.empty());

        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001", "customeremail", "john@example.com",
                "productsku", "MISSING-SKU", "quantity", "1", "paymentmethod", "CASH"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e ->
                ImportValidationCode.ENTITY_NOT_FOUND.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Invalid quantity — returns failure with INVALID_FORMAT before hitting DB")
    void invalidQuantity_returnsFailure() {
        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001", "customeremail", "john@example.com",
                "productsku", "SKU-001", "quantity", "not-a-number", "paymentmethod", "CASH"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e ->
                ImportValidationCode.INVALID_FORMAT.equals(e.errorCode()));
        then(customerRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Invalid payment method — returns failure with INVALID_VALUE")
    void invalidPaymentMethod_returnsFailure() {
        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001", "customeremail", "john@example.com",
                "productsku", "SKU-001", "quantity", "1", "paymentmethod", "BITCOIN"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e ->
                ImportValidationCode.INVALID_VALUE.equals(e.errorCode()));
    }

    @Test
    @DisplayName("BusinessRuleException from OrderService — returns failure with BUSINESS_RULE_VIOLATION")
    void businessRuleViolation_returnsFailure() {
        Customer customer = mockActiveCustomer();
        Product product = mockActiveProduct("SKU-001");
        given(customerRepository.findByEmail("john@example.com")).willReturn(Optional.of(customer));
        given(productRepository.findBySku("SKU-001")).willReturn(Optional.of(product));
        given(orderService.createOrder(any()))
                .willThrow(new BusinessRuleException(ErrorCode.INSUFFICIENT_STOCK, "Not enough stock"));

        List<ParsedRow> rows = List.of(row(1, Map.of(
                "ordernumber", "ORD-001", "customeremail", "john@example.com",
                "productsku", "SKU-001", "quantity", "999999", "paymentmethod", "CASH"
        )));

        RowImportResult result = service.importOrderGroup("ORD-001", rows);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e ->
                ImportValidationCode.BUSINESS_RULE_VIOLATION.equals(e.errorCode()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ParsedRow row(int rowNumber, Map<String, String> values) {
        return new ParsedRow(rowNumber, values);
    }

    private Customer mockActiveCustomer() {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setStatus(CustomerStatus.ACTIVE);
        return c;
    }

    private Product mockActiveProduct(String sku) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setSku(sku);
        p.setActive(true);
        return p;
    }
}
