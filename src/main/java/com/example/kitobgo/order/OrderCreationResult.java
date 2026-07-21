package com.example.kitobgo.order;

import com.example.kitobgo.order.dto.OrderResponseDto;

/** created=false — ayni Idempotency-Key uchun oldin yaratilgan order qaytarildi. */
public record OrderCreationResult(
        OrderResponseDto order,
        boolean created
) {
}
