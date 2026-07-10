package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.Product;
import com.example.kitobgo.product.ProductImage;

import java.util.List;
import java.util.UUID;

public record ProductResponseDto(
        UUID id,
        String name,
        String title,
        String author,
        Integer price,
        Float rating,
        Integer pageCount,
        Integer publishedYear,
        Integer stockQuantity,
        List<String> images
) {
    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getTitle(),
                product.getAuthor(),
                product.getPrice(),
                product.getRating(),
                product.getPageCount(),
                product.getPublishedYear(),
                product.getStockQuantity(),
                product.getImages().stream()
                        .map(ProductImage::getUrl)
                        .toList()
        );
    }
}
