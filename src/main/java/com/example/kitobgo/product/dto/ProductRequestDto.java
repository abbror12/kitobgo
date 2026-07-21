package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record ProductRequestDto(
        @NotBlank(message = "Kitob nomi bo'sh bo'lishi mumkin emas")
        String title,

        String description,

        String author,

        @Size(max = 20, message = "ISBN 20 belgidan uzun bo'lishi mumkin emas")
        String isbn,

        @Size(max = 255, message = "Nashriyot nomi 255 belgidan uzun bo'lishi mumkin emas")
        String publisher,

        @Size(max = 3, message = "Til kodi 2 yoki 3 harfdan iborat bo'lishi kerak")
        String language,

        ProductStatus status,

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

        Set<@NotNull(message = "Kategoriya ID'si bo'sh bo'lishi mumkin emas") Long> categoryIds,

        List<@NotBlank(message = "Rasm URL'i bo'sh bo'lishi mumkin emas") String> imageUrls
) {
}
