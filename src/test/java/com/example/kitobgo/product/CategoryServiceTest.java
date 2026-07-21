package com.example.kitobgo.product;

import com.example.kitobgo.common.ConflictException;
import com.example.kitobgo.product.dto.CategoryRequestDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    @Test
    void createsCategoryWithNormalizedName() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });
        CategoryService service = new CategoryService(repository);

        var response = service.create(new CategoryRequestDto("  Badiiy   adabiyot "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Badiiy adabiyot");
        verify(repository).existsByNameIgnoreCase("Badiiy adabiyot");
    }

    @Test
    void duplicateCategoryIsRejectedCaseInsensitively() {
        CategoryRepository repository = mock(CategoryRepository.class);
        when(repository.existsByNameIgnoreCase("Badiiy adabiyot")).thenReturn(true);
        CategoryService service = new CategoryService(repository);

        assertThatThrownBy(() -> service.create(new CategoryRequestDto("Badiiy adabiyot")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("allaqachon mavjud");
    }
}
