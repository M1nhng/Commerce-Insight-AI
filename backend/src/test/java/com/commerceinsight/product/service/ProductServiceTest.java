package com.commerceinsight.product.service;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.category.repository.CategoryRepository;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.product.domain.Product;
import com.commerceinsight.product.dto.request.CreateProductRequest;
import com.commerceinsight.product.dto.request.UpdateProductRequest;
import com.commerceinsight.product.dto.response.ProductResponse;
import com.commerceinsight.product.dto.response.ProductSummaryResponse;
import com.commerceinsight.product.mapper.ProductMapper;
import com.commerceinsight.product.repository.ProductRepository;
import com.commerceinsight.shared.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductServiceTest — unit tests for ProductService business logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Spy  private ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
    @InjectMocks private ProductService productService;

    private UUID productId;
    private UUID categoryId;
    private Product existingProduct;
    private Category category;

    @BeforeEach
    void setUp() {
        productId  = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = Category.builder().name("Electronics").slug("electronics").build();
        category.setId(categoryId);

        existingProduct = Product.builder()
                .sku("SKU-001")
                .name("Wireless Headphones Pro")
                .price(new BigDecimal("49.99"))
                .category(category)
                .active(true)
                .build();
        existingProduct.setId(productId);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("should return paginated product list")
        void findAll_returnsPage() {
            Page<Product> productPage = new PageImpl<>(List.of(existingProduct),
                    PageRequest.of(0, 10), 1);
            when(productRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(productPage);

            PageResponse<ProductSummaryResponse> response = productService.findAll(
                    null, null, null, null, null, PageRequest.of(0, 10));

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
            assertThat(response.getContent().get(0).sku()).isEqualTo("SKU-001");
        }
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return full product when found")
        void findById_found_returnsFullResponse() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

            ProductResponse response = productService.findById(productId);

            assertThat(response.id()).isEqualTo(productId);
            assertThat(response.sku()).isEqualTo("SKU-001");
            assertThat(response.categoryName()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when product not found")
        void findById_notFound_throwsException() {
            when(productRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.findById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create product and return full response")
        void create_validRequest_returns201Response() {
            CreateProductRequest request = new CreateProductRequest(
                    "sku-002", "New Product", null,
                    new BigDecimal("29.99"), null, null, categoryId, 10);

            when(productRepository.existsBySku("SKU-002")).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            ProductResponse response = productService.create(request);

            assertThat(response.sku()).isEqualTo("SKU-002");
            assertThat(response.name()).isEqualTo("New Product");
            assertThat(response.categoryId()).isEqualTo(categoryId);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when SKU already exists")
        void create_duplicateSku_throws409() {
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-001", "Duplicate", null,
                    new BigDecimal("10.00"), null, null, null, 0);

            when(productRepository.existsBySku("SKU-001")).thenReturn(true);

            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("SKU-001");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for unknown categoryId")
        void create_unknownCategory_throws404() {
            UUID unknownCat = UUID.randomUUID();
            CreateProductRequest request = new CreateProductRequest(
                    "SKU-999", "New", null, new BigDecimal("5.00"),
                    null, null, unknownCat, 0);

            when(productRepository.existsBySku("SKU-999")).thenReturn(false);
            when(categoryRepository.findById(unknownCat)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should normalize SKU to uppercase on create")
        void create_lowercaseSku_isNormalizedToUppercase() {
            CreateProductRequest request = new CreateProductRequest(
                    "sku-lower", "Product", null,
                    new BigDecimal("9.99"), null, null, null, 0);

            when(productRepository.existsBySku("SKU-LOWER")).thenReturn(false);
            when(productRepository.save(any())).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            ProductResponse response = productService.create(request);
            assertThat(response.sku()).isEqualTo("SKU-LOWER");
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update product successfully")
        void update_validRequest_updatesAndReturns() {
            UpdateProductRequest request = new UpdateProductRequest(
                    "SKU-001", "Updated Name", "New description",
                    new BigDecimal("59.99"), null, null, categoryId, true);

            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(productRepository.save(any())).thenReturn(existingProduct);

            ProductResponse response = productService.update(productId, request);

            assertThat(response.name()).isEqualTo("Updated Name");
            assertThat(response.price()).isEqualByComparingTo("59.99");
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new SKU conflicts")
        void update_skuConflict_throws409() {
            UpdateProductRequest request = new UpdateProductRequest(
                    "SKU-TAKEN", "Name", null,
                    new BigDecimal("10.00"), null, null, null, true);

            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.existsBySkuAndIdNot("SKU-TAKEN", productId)).thenReturn(true);

            assertThatThrownBy(() -> productService.update(productId, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should soft-delete product")
        void delete_existingProduct_softDeletes() {
            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(productRepository.save(any())).thenReturn(existingProduct);

            productService.delete(productId);

            assertThat(existingProduct.getDeletedAt()).isNotNull();
            verify(productRepository).save(existingProduct);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when deleting nonexistent product")
        void delete_notFound_throwsException() {
            when(productRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.delete(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
