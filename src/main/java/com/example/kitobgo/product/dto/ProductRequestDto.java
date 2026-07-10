package com.example.kitobgo.product.dto;

import java.util.List;

public record ProductRequestDto(
        String title,
        String description,
        String author,
        Integer price,
        Integer discountPrice,
        Integer pageCount,
        Integer publishedYear,
        Integer stockQuantity,
        List<String> imageUrls
) {
}
