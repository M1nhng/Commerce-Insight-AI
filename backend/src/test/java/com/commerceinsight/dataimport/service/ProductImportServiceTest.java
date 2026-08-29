package com.commerceinsight.dataimport.service;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.category.repository.CategoryRepository;
import com.commerceinsight.dataimport.parser.ParsedRow;
import com.commerceinsight.dataimport.validation.ImportValidationCode;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.product.dto.request.CreateProductRequest;
import com.commerceinsight.product.dto.response.ProductResponse;
import com.commerceinsight.product.service.ProductService;
import com.commerceinsight.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ProductImportServiceTest — unit tests for {@link ProductImportService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductImportService Unit Tests")
class ProductImportServiceTest {

    @Mock private ProductService productService;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private ProductImportService service;

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid row — calls ProductService.create, returns success")
    void validRow_returnsSuccess() {
        ParsedRow row = row(1, Map.of(
                "sku", "SKU-001",
                "name", "Widget",
                "price", "100.00"
        ));
        given(productService.create(any())).willReturn(null); // response not checked

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isTrue();
        assertThat(result.rowNumber()).isEqualTo(1);
        then(productService).should().create(any(CreateProductRequest.class));
    }

    @Test
    @DisplayName("Valid row with category name — resolves category by name")
    void validRowWithCategory_resolvesByName() {
        Category cat = new Category();
        cat.setId(UUID.randomUUID());
        given(categoryRepository.findByNameIgnoreCase("Electronics")).willReturn(Optional.of(cat));
        given(productService.create(any())).willReturn(null);

        ParsedRow row = row(1, Map.of(
                "sku", "SKU-001", "name", "Widget", "price", "100.00", "categoryname", "Electronics"
        ));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isTrue();
        then(categoryRepository).should().findByNameIgnoreCase("Electronics");
    }

    @Test
    @DisplayName("Category not found — returns failure with ENTITY_NOT_FOUND")
    void categoryNotFound_returnsFailure() {
        given(categoryRepository.findByNameIgnoreCase("Unknown")).willReturn(Optional.empty());

        ParsedRow row = row(1, Map.of(
                "sku", "SKU-001", "name", "Widget", "price", "100.00", "categoryname", "Unknown"
        ));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors().get(0).errorCode()).isEqualTo(ImportValidationCode.ENTITY_NOT_FOUND);
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Test
    @DisplayName("Missing SKU — returns failure with MISSING_REQUIRED_FIELD")
    void missingSku_returnsFailure() {
        ParsedRow row = row(2, Map.of("name", "Widget", "price", "100.00"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "sku".equals(e.fieldName())
                && ImportValidationCode.MISSING_REQUIRED_FIELD.equals(e.errorCode()));
        then(productService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Missing name — returns failure with MISSING_REQUIRED_FIELD")
    void missingName_returnsFailure() {
        ParsedRow row = row(3, Map.of("sku", "SKU-001", "price", "100.00"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "name".equals(e.fieldName()));
    }

    @Test
    @DisplayName("Invalid price format — returns failure with INVALID_FORMAT")
    void invalidPriceFormat_returnsFailure() {
        ParsedRow row = row(4, Map.of("sku", "SKU-001", "name", "Widget", "price", "not-a-number"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> "price".equals(e.fieldName())
                && ImportValidationCode.INVALID_FORMAT.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Negative price — returns failure with NEGATIVE_VALUE")
    void negativePrice_returnsFailure() {
        ParsedRow row = row(5, Map.of("sku", "SKU-001", "name", "Widget", "price", "-10.00"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> ImportValidationCode.NEGATIVE_VALUE.equals(e.errorCode()));
    }

    @Test
    @DisplayName("Duplicate SKU — returns failure with DUPLICATE_RECORD")
    void duplicateSku_returnsFailure() {
        given(productService.create(any()))
                .willThrow(new DuplicateResourceException(ErrorCode.DUPLICATE_SKU, "SKU already exists"));

        ParsedRow row = row(6, Map.of("sku", "EXISTING-SKU", "name", "Widget", "price", "100.00"));

        RowImportResult result = service.importRow(row);

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).anyMatch(e -> ImportValidationCode.DUPLICATE_RECORD.equals(e.errorCode()));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ParsedRow row(int rowNumber, Map<String, String> values) {
        return new ParsedRow(rowNumber, values);
    }
}
