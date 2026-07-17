package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.ProductImage;


public record ProductImageResponseDto(
        Long id,
        String url,
        Integer sortOrder
) {
    public static ProductImageResponseDto from(ProductImage image) {
        return new ProductImageResponseDto(
                image.getId(),
                image.getUrl(),
                image.getSortOrder()
        );
    }
}
