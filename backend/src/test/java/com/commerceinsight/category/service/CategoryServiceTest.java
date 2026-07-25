package com.commerceinsight.category.service;

import com.commerceinsight.category.domain.Category;
import com.commerceinsight.category.dto.request.CreateCategoryRequest;
import com.commerceinsight.category.dto.request.UpdateCategoryRequest;
import com.commerceinsight.category.dto.response.CategoryResponse;
import com.commerceinsight.category.mapper.CategoryMapper;
import com.commerceinsight.category.repository.CategoryRepository;
import com.commerceinsight.exception.BusinessRuleException;
import com.commerceinsight.exception.DuplicateResourceException;
import com.commerceinsight.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CategoryServiceTest — unit tests for CategoryService business logic.
 *
 * <p>All dependencies are mocked. No Spring context or database required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Spy  private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
    @InjectMocks private CategoryService categoryService;

    private UUID categoryId;
    private Category existingCategory;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        existingCategory = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("All electronics")
                .sortOrder(0)
                .active(true)
                .build();
        // Simulate JPA-assigned ID
        existingCategory.setId(categoryId);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("should return category when found")
        void findById_found_returnsResponse() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.countActiveProductsByCategoryId(categoryId)).thenReturn(5L);

            CategoryResponse response = categoryService.findById(categoryId);

            assertThat(response.id()).isEqualTo(categoryId);
            assertThat(response.name()).isEqualTo("Electronics");
            assertThat(response.productCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void findById_notFound_throwsException() {
            when(categoryRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("was not found");
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create root category with auto-generated slug")
        void create_rootCategory_success() {
            CreateCategoryRequest request = new CreateCategoryRequest(
                    "Mobile Phones", null, null, 0);

            when(categoryRepository.existsBySlug("mobile-phones")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });
            when(categoryRepository.countActiveProductsByCategoryId(any())).thenReturn(0L);

            CategoryResponse response = categoryService.create(request);

            assertThat(response.name()).isEqualTo("Mobile Phones");
            assertThat(response.slug()).isEqualTo("mobile-phones");
            assertThat(response.parentId()).isNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("should append suffix when slug is already taken")
        void create_duplicateSlug_appendsSuffix() {
            CreateCategoryRequest request = new CreateCategoryRequest(
                    "Electronics", null, null, 0);

            when(categoryRepository.existsBySlug("electronics")).thenReturn(true);
            when(categoryRepository.existsBySlug("electronics-2")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });
            when(categoryRepository.countActiveProductsByCategoryId(any())).thenReturn(0L);

            CategoryResponse response = categoryService.create(request);

            assertThat(response.slug()).isEqualTo("electronics-2");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when parentId does not exist")
        void create_invalidParent_throwsException() {
            UUID unknownParent = UUID.randomUUID();
            CreateCategoryRequest request = new CreateCategoryRequest(
                    "Sub-Category", null, unknownParent, 0);

            when(categoryRepository.findById(unknownParent)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should soft-delete category when no products or children")
        void delete_emptyCategory_success() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.countActiveProductsByCategoryId(categoryId)).thenReturn(0L);
            when(categoryRepository.existsByParentId(categoryId)).thenReturn(false);
            when(categoryRepository.save(any())).thenReturn(existingCategory);

            categoryService.delete(categoryId);

            assertThat(existingCategory.getDeletedAt()).isNotNull();
            verify(categoryRepository).save(existingCategory);
        }

        @Test
        @DisplayName("should throw CATEGORY_HAS_PRODUCTS when active products exist")
        void delete_withProducts_throwsBusinessRuleException() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.countActiveProductsByCategoryId(categoryId)).thenReturn(3L);

            assertThatThrownBy(() -> categoryService.delete(categoryId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("3 active product(s)");
        }

        @Test
        @DisplayName("should throw exception when category has child categories")
        void delete_withChildren_throwsBusinessRuleException() {
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.countActiveProductsByCategoryId(categoryId)).thenReturn(0L);
            when(categoryRepository.existsByParentId(categoryId)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.delete(categoryId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("child categories");
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update category name and regenerate slug")
        void update_nameChange_regeneratesSlug() {
            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Consumer Electronics", null, null, 0, true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.existsBySlugAndIdNot("consumer-electronics", categoryId))
                    .thenReturn(false);
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(categoryRepository.countActiveProductsByCategoryId(any())).thenReturn(0L);

            CategoryResponse response = categoryService.update(categoryId, request);

            assertThat(response.name()).isEqualTo("Consumer Electronics");
            assertThat(response.slug()).isEqualTo("consumer-electronics");
        }

        @Test
        @DisplayName("should throw CATEGORY_CIRCULAR_REFERENCE when re-parenting to descendant")
        void update_circularReference_throwsException() {
            UUID childId = UUID.randomUUID();
            Category child = Category.builder().name("Child").slug("child").build();
            child.setId(childId);
            child.setParent(existingCategory);

            UpdateCategoryRequest request = new UpdateCategoryRequest(
                    "Electronics", null, childId, 0, true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
            // Child's parent is existingCategory — walking up: childId → categoryId (matches!)
            when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));

            assertThatThrownBy(() -> categoryService.update(categoryId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("circular reference");
        }
    }
}
