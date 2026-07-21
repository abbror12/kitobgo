package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponseDto(
        Long id,
        String title,
        String description,
        String author,
        String isbn,
        String publisher,
        String language,
        ProductStatus status,
        Integer price,
        Integer discountPrice,
        boolean hasDiscount,
        Float rating,
        Integer pageCount,
        Integer publishedYear,
        Integer stockQuantity,
        boolean inStock,
        List<CategoryResponseDto> categories,
        List<ProductImageResponseDto> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getAuthor(),
                product.getIsbn(),
                product.getPublisher(),
                product.getLanguage(),
                product.getStatus(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.hasDiscount(),
                product.getRating(),
                product.getPageCount(),
                product.getPublishedYear(),
                product.getStockQuantity(),
                product.isInStock(),
                product.getCategories().stream()
                        .map(CategoryResponseDto::from)
                        .toList(),
                product.getImages().stream()
                        .map(ProductImageResponseDto::from)
                        .toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
