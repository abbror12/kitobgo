package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.Product;

import java.util.List;

public record ProductResponseDto(
        Long id,
        String title,
        String description,
        String author,
        Integer price,
        Integer discountPrice,
        boolean hasDiscount,
        Float rating,
        Integer pageCount,
        Integer publishedYear,
        Integer stockQuantity,
        boolean inStock,
        List<ProductImageResponseDto> images
) {
    public static ProductResponseDto from(Product product) {
        Integer stock = product.getStockQuantity();
        Integer price = product.getPrice();
        Integer discountPrice = product.getDiscountPrice();
        // Chegirma bor: chegirma narxi kiritilgan va asl narxdan past bo'lsa.
        boolean hasDiscount = discountPrice != null && price != null && discountPrice < price;
        return new ProductResponseDto(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getAuthor(),
                price,
                discountPrice,
                hasDiscount,
                product.getRating(),
                product.getPageCount(),
                product.getPublishedYear(),
                stock,
                stock != null && stock > 0,
                product.getImages().stream()
                        .map(ProductImageResponseDto::from)
                        .toList()
        );
    }
}
