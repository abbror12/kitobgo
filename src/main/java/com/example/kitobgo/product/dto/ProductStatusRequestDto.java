package com.example.kitobgo.product.dto;

import com.example.kitobgo.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequestDto(
        @NotNull(message = "Mahsulot statusi ko'rsatilishi shart")
        ProductStatus status
) {
}
