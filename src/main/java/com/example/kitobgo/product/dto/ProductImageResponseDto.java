package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.ProductImage;

import java.util.UUID;

public record ProductImageResponseDto(
        UUID id,
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
