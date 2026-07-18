package com.example.kitobgo.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Buyurtma qatori. {@code quantity} berilmasa 1 deb olinadi
 * ({@code OrderService.populateItems}), shuning uchun u majburiy emas.
 */
public record OrderItemRequest(
        @NotNull(message = "Mahsulot ID ko'rsatilishi shart")
        Long productId,

        @Positive(message = "Miqdor musbat bo'lishi kerak")
        Integer quantity
) {
}
