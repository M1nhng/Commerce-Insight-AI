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
import com.commerceinsight.product.repository.ProductSpecification;
import com.commerceinsight.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ProductService — all business logic for the product domain.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>No JPA entity leaves this service — DTOs only.</li>
 *   <li>All write operations are transactional.</li>
 *   <li>SKU uniqueness checked in service layer BEFORE hitting the DB constraint.</li>
 *   <li>Category FK validated before saving to produce a meaningful 404 instead of a DB error.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    // ── Queries ──────────────────────────────────────────────────────────────

    /**
     * Paginated, filterable product list.
     *
     * @param search     optional substring search on name / SKU
     * @param categoryId optional category filter
     * @param active     optional active flag (null = both)
     * @param priceMin   optional minimum price
     * @param priceMax   optional maximum price
     * @param pageable   pagination and sort
     */
    public PageResponse<ProductSummaryResponse> findAll(
            String search,
            UUID categoryId,
            Boolean active,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Pageable pageable
    ) {
        Specification<Product> spec = ProductSpecification.of(search, categoryId, active, priceMin, priceMax);
        Page<Product> page = productRepository.findAll(spec, pageable);
        Page<ProductSummaryResponse> dtoPage = page.map(productMapper::toSummary);
        return PageResponse.from(dtoPage);
    }

    /**
     * Single product by ID.
     *
     * @throws ResourceNotFoundException if not found or soft-deleted
     */
    public ProductResponse findById(UUID id) {
        return productMapper.toResponse(getOrThrow(id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Create a new product.
     *
     * @throws DuplicateResourceException (409) if SKU already exists
     * @throws ResourceNotFoundException  (404) if categoryId is specified but not found
     */
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        // Guard: unique SKU (normalize to uppercase consistently)
        String normalizedSku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(normalizedSku)) {
            throw DuplicateResourceException.sku(request.sku());
        }

        Category category = resolveCategory(request.categoryId());

        Product product = Product.builder()
                .sku(normalizedSku)
                .name(request.name().trim())
                .description(request.description())
                .price(request.price())
                .costPrice(request.costPrice())
                .imageUrl(request.imageUrl())
                .category(category)
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    /**
     * Full update of an existing product.
     *
     * @throws ResourceNotFoundException  (404) if product not found
     * @throws DuplicateResourceException (409) if new SKU conflicts with another product
     * @throws ResourceNotFoundException  (404) if categoryId is specified but not found
     */
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = getOrThrow(id);

        // Guard: unique SKU (allow same SKU on same product)
        String newSku = request.sku().trim().toUpperCase();
        if (!newSku.equals(product.getSku()) && productRepository.existsBySkuAndIdNot(newSku, id)) {
            throw DuplicateResourceException.sku(request.sku());
        }

        Category category = resolveCategory(request.categoryId());

        product.setSku(newSku);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCostPrice(request.costPrice());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        product.setActive(request.resolvedActive());

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", saved.getId());
        return productMapper.toResponse(saved);
    }

    /**
     * Soft-delete a product (ADMIN only — enforced at controller level).
     *
     * @throws ResourceNotFoundException if not found
     */
    @Transactional
    public void delete(UUID id) {
        Product product = getOrThrow(id);
        product.softDelete();
        productRepository.save(product);
        log.info("Product soft-deleted: id={}, sku={}", id, product.getSku());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Product getOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.product(id));
    }

    /**
     * Resolve a nullable category UUID to a Category entity.
     * Returns null if categoryId is null (product without category is valid).
     */
    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.category(categoryId));
    }
}
