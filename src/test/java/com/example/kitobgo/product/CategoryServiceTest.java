package com.example.kitobgo.product;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.common.NotFoundException;
import com.example.kitobgo.product.dto.CategoryRequestDto;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    @Test
    void createsCategoryWithNormalizedName() {
        CategoryRepository repository = mock(CategoryRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        when(repository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });
        CategoryService service = new CategoryService(repository, productRepository);

        var response = service.create(new CategoryRequestDto("  Badiiy   adabiyot "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Badiiy adabiyot");
        verify(repository).existsByNameIgnoreCase("Badiiy adabiyot");
    }

    @Test
    void duplicateCategoryIsRejectedCaseInsensitively() {
        CategoryRepository repository = mock(CategoryRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        when(repository.existsByNameIgnoreCase("Badiiy adabiyot")).thenReturn(true);
        CategoryService service = new CategoryService(repository, productRepository);

        assertThatThrownBy(() -> service.create(new CategoryRequestDto("Badiiy adabiyot")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("allaqachon mavjud");
    }

    @Test
    void deletesCategoryWhenNoProductsAttached() {
        CategoryRepository repository = mock(CategoryRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        Category category = Category.builder().id(5L).name("Bo'sh kategoriya").build();
        when(repository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategories_Id(5L)).thenReturn(false);
        CategoryService service = new CategoryService(repository, productRepository);

        service.delete(5L);

        verify(repository).delete(category);
    }

    @Test
    void deleteIsRejectedWhenProductsStillAttached() {
        CategoryRepository repository = mock(CategoryRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        Category category = Category.builder().id(5L).name("Badiiy adabiyot").build();
        when(repository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategories_Id(5L)).thenReturn(true);
        CategoryService service = new CategoryService(repository, productRepository);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("biriktirilgan");
        verify(repository, never()).delete(any(Category.class));
    }

    @Test
    void deleteMissingCategoryThrowsNotFound() {
        CategoryRepository repository = mock(CategoryRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        when(repository.findById(404L)).thenReturn(Optional.empty());
        CategoryService service = new CategoryService(repository, productRepository);

        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).existsByCategories_Id(any());
    }
}
