package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.Category;

public record CategoryResponseDto(
        Long id,
        String name
) {
    public static CategoryResponseDto from(Category category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }
}
