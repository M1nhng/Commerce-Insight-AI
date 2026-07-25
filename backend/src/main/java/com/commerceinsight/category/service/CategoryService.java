package com.commerceinsight.category.service;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.category.dto.request.CreateCategoryRequest;
import com.commerceinsight.category.dto.request.UpdateCategoryRequest;
import com.commerceinsight.category.dto.response.CategoryResponse;
import com.commerceinsight.category.dto.response.CategoryTreeResponse;
import com.commerceinsight.category.mapper.CategoryMapper;
import com.commerceinsight.category.repository.CategoryRepository;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
import com.commerceinsight.shared.dto.PageResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.commerceinsight.shared.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * CategoryService — all business logic for the category domain.
 *
 * <p>Architecture Rules:
 * <ul>
 *   <li>No JPA entity leaves this service — only DTOs are returned.</li>
 *   <li>All write operations are transactional.</li>
 *   <li>Slug generation and uniqueness enforcement happen here, not in the controller.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // ── Queries ──────────────────────────────────────────────────────────────

    /**
     * Paginated flat list of all active categories.
     *
     * @param search optional name substring filter
     * @param pageable Spring Data pagination/sort
     */
    public PageResponse<CategoryResponse> findAll(String search, Pageable pageable) {
        var page = categoryRepository.findAllWithSearch(
                (search == null || search.isBlank()) ? null : search.trim(),
                pageable
        );
        var content = page.getContent().stream()
                .map(c -> {
                    long count = categoryRepository.countActiveProductsByCategoryId(c.getId());
                    CategoryResponse resp = categoryMapper.toResponse(c);
                    // ProductCount is not in entity — rebuild with count
                    return new CategoryResponse(
                            resp.id(), resp.name(), resp.slug(), resp.description(),
                            resp.parentId(), resp.sortOrder(), resp.active(),
                            count, resp.createdAt()
                    );
                })
                .collect(Collectors.toList());
        return PageResponse.of(content, page);
    }

    /**
     * Full category tree — root categories with recursively nested children.
     */
    public List<CategoryTreeResponse> findTree() {
        List<Category> roots = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        return roots.stream()
                .map(this::buildTree)
                .collect(Collectors.toList());
    }

    /**
     * Single category by ID.
     *
     * @throws ResourceNotFoundException if not found or soft-deleted
     */
    public CategoryResponse findById(UUID id) {
        Category category = getOrThrow(id);
        long count = categoryRepository.countActiveProductsByCategoryId(id);
        CategoryResponse resp = categoryMapper.toResponse(category);
        return new CategoryResponse(
                resp.id(), resp.name(), resp.slug(), resp.description(),
                resp.parentId(), resp.sortOrder(), resp.active(),
                count, resp.createdAt()
        );
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Create a new category.
     *
     * @throws ResourceNotFoundException if parentId is specified but not found
     * @throws DuplicateResourceException if the generated slug is already in use
     */
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        // Resolve parent
        Category parent = resolveParent(request.parentId());

        // Generate & validate slug
        String slug = generateUniqueSlug(request.name(), null);

        Category category = Category.builder()
                .name(request.name().trim())
                .slug(slug)
                .description(request.description())
                .parent(parent)
                .sortOrder(request.resolvedSortOrder())
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, slug={}", saved.getId(), saved.getSlug());
        return mapWithCount(saved);
    }

    /**
     * Full update of an existing category.
     *
     * @throws ResourceNotFoundException if category or parentId not found
     * @throws BusinessRuleException if the update would create a circular reference
     */
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = getOrThrow(id);

        // Circular reference guard: new parent cannot be a descendant of this category
        if (request.parentId() != null && !request.parentId().equals(
                category.getParent() != null ? category.getParent().getId() : null)) {
            validateNoCircularReference(id, request.parentId());
        }

        Category parent = resolveParent(request.parentId());

        // Re-slug only if name changed
        String slug = request.name().trim().equals(category.getName())
                ? category.getSlug()
                : generateUniqueSlug(request.name(), id);

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setParent(parent);
        category.setSortOrder(request.resolvedSortOrder());
        category.setActive(request.resolvedActive());

        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}", saved.getId());
        return mapWithCount(saved);
    }

    /**
     * Soft-delete a category.
     *
     * @throws ResourceNotFoundException if not found
     * @throws BusinessRuleException if the category has active products (CATEGORY_HAS_PRODUCTS)
     * @throws BusinessRuleException if the category has active child categories
     */
    @Transactional
    public void delete(UUID id) {
        Category category = getOrThrow(id);

        // Guard: cannot delete if products exist in this category
        long productCount = categoryRepository.countActiveProductsByCategoryId(id);
        if (productCount > 0) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_HAS_PRODUCTS,
                    "Cannot delete category '" + category.getName() + "' — it contains "
                            + productCount + " active product(s). Move or delete them first."
            );
        }

        // Guard: cannot delete if it has active child categories
        if (categoryRepository.existsByParentId(id)) {
            throw new BusinessRuleException(
                    ErrorCode.CATEGORY_HAS_PRODUCTS,
                    "Cannot delete category '" + category.getName()
                            + "' — it still has child categories. Delete or re-parent them first."
            );
        }

        category.softDelete();
        categoryRepository.save(category);
        log.info("Category soft-deleted: id={}", id);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Category getOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Category with ID '" + id + "' was not found"
                ));
    }

    private Category resolveParent(UUID parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.CATEGORY_NOT_FOUND,
                        "Parent category with ID '" + parentId + "' was not found"
                ));
    }

    /**
     * Generate a unique slug. If the base slug is taken, append -2, -3, … until unique.
     *
     * @param name      raw name input
     * @param excludeId category ID to exclude from uniqueness check (null for new categories)
     */
    private String generateUniqueSlug(String name, UUID excludeId) {
        String baseSlug = SlugUtil.slugify(name);
        String candidate = baseSlug;
        int suffix = 2;

        while (isSlugTaken(candidate, excludeId)) {
            candidate = SlugUtil.slugifyWithSuffix(baseSlug, suffix++);
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, UUID excludeId) {
        if (excludeId == null) {
            return categoryRepository.existsBySlug(slug);
        }
        return categoryRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    /**
     * Recursively build a tree node from a category entity.
     */
    private CategoryTreeResponse buildTree(Category category) {
        long count = categoryRepository.countActiveProductsByCategoryId(category.getId());
        List<Category> children = categoryRepository
                .findByParentIdOrderBySortOrderAsc(category.getId());

        List<CategoryTreeResponse> childResponses = children.stream()
                .map(this::buildTree)
                .collect(Collectors.toList());

        return categoryMapper.toTreeResponse(category, count, childResponses);
    }

    /**
     * Guard against circular references when re-parenting a category.
     * Walks up the ancestor chain of the proposed new parent to ensure this
     * category does not appear in it.
     */
    private void validateNoCircularReference(UUID categoryId, UUID proposedParentId) {
        UUID cursor = proposedParentId;
        while (cursor != null) {
            if (cursor.equals(categoryId)) {
                throw new BusinessRuleException(
                        ErrorCode.CATEGORY_CIRCULAR_REFERENCE,
                        "Cannot set parent: it would create a circular reference in the category tree"
                );
            }
            Category ancestor = categoryRepository.findById(cursor).orElse(null);
            cursor = (ancestor != null && ancestor.getParent() != null)
                    ? ancestor.getParent().getId()
                    : null;
        }
    }

    private CategoryResponse mapWithCount(Category category) {
        long count = categoryRepository.countActiveProductsByCategoryId(category.getId());
        CategoryResponse resp = categoryMapper.toResponse(category);
        return new CategoryResponse(
                resp.id(), resp.name(), resp.slug(), resp.description(),
                resp.parentId(), resp.sortOrder(), resp.active(),
                count, resp.createdAt()
        );
    }
}
