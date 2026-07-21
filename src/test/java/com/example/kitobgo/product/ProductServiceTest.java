package com.example.kitobgo.product;

import com.example.kitobgo.product.dto.ProductRequestDto;
import com.example.kitobgo.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    @Test
    void archiveKeepsProductAndItsFiles() {
        ProductRepository repository = mock(ProductRepository.class);
        FileStorageService storage = mock(FileStorageService.class);
        Product product = Product.builder()
                .id(42L)
                .status(ProductStatus.ACTIVE)
                .build();
        when(repository.findById(42L)).thenReturn(Optional.of(product));

        ProductService service = new ProductService(repository, storage, mock(CategoryRepository.class));
        service.archive(42L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
        verify(repository).save(product);
        verify(repository, never()).delete(product);
        verify(storage, never()).deleteAfterCommit(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void archivedProductCanBeRestoredToActive() {
        ProductRepository repository = mock(ProductRepository.class);
        Product product = Product.builder()
                .id(42L)
                .status(ProductStatus.ARCHIVED)
                .build();
        when(repository.findById(42L)).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);

        ProductService service = new ProductService(
                repository, mock(FileStorageService.class), mock(CategoryRepository.class));
        service.changeStatus(42L, ProductStatus.ACTIVE);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        verify(repository).save(product);
    }

    @Test
    void publicCatalogLoadsOnlyActiveProducts() {
        ProductRepository repository = mock(ProductRepository.class);
        PageRequest pageable = PageRequest.of(0, 12);
        when(repository.findAllByStatus(ProductStatus.ACTIVE, pageable)).thenReturn(Page.empty(pageable));

        ProductService service = new ProductService(
                repository, mock(FileStorageService.class), mock(CategoryRepository.class));
        service.getAll(pageable);

        verify(repository).findAllByStatus(ProductStatus.ACTIVE, pageable);
        verify(repository, never()).findAll(pageable);
    }

    @Test
    void updateReplacesProductCategoriesByIds() {
        ProductRepository repository = mock(ProductRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        Product product = Product.builder().id(42L).status(ProductStatus.ACTIVE).build();
        Category category = Category.builder().id(7L).name("Badiiy adabiyot").build();
        when(repository.findById(42L)).thenReturn(Optional.of(product));
        when(repository.save(product)).thenReturn(product);
        when(categoryRepository.findAllById(Set.of(7L))).thenReturn(List.of(category));
        ProductRequestDto request = new ProductRequestDto(
                "O'tkan kunlar", null, "Abdulla Qodiriy", null, null, "uz",
                ProductStatus.ACTIVE, 100_000, null, 400, 1926, 10,
                Set.of(7L), null);

        ProductService service = new ProductService(
                repository, mock(FileStorageService.class), categoryRepository);
        var response = service.update(42L, request);

        assertThat(product.getCategories()).containsExactly(category);
        assertThat(response.categories()).singleElement()
                .satisfies(item -> assertThat(item.id()).isEqualTo(7L));
    }
}
