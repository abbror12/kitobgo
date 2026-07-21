package com.example.kitobgo.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDto(
        @NotBlank(message = "Kategoriya nomi bo'sh bo'lishi mumkin emas")
        @Size(max = 100, message = "Kategoriya nomi 100 belgidan uzun bo'lishi mumkin emas")
        String name
) {
}
