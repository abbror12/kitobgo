package com.example.kitobgo.product.dto;

import java.util.List;

public record ProductRequestDto(
        String name,
        String title,
        String author,
        Integer price,
        Integer pageCount,
        Integer publishedYear,
        Integer stockQuantity,
        List<String> imageUrls
) {
}
