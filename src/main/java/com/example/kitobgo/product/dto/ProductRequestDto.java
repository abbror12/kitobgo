package com.example.kitobgo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ProductRequestDto(
        @NotBlank(message = "Kitob nomi bo'sh bo'lishi mumkin emas")
        String title,

        String description,

        String author,

        @NotNull(message = "Narx ko'rsatilishi shart")
        @Positive(message = "Narx musbat bo'lishi kerak")
        Integer price,

        @Positive(message = "Chegirma narxi musbat bo'lishi kerak")
        Integer discountPrice,

        @Positive(message = "Sahifalar soni musbat bo'lishi kerak")
        Integer pageCount,

        @Positive(message = "Nashr yili musbat bo'lishi kerak")
        Integer publishedYear,

        @PositiveOrZero(message = "Zaxira miqdori manfiy bo'lishi mumkin emas")
        Integer stockQuantity,

        List<@NotBlank(message = "Rasm URL'i bo'sh bo'lishi mumkin emas") String> imageUrls
) {
}
