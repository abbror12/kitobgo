package com.example.kitobgo.order.dto;

import java.util.UUID;

public record OrderRequestDto(
        UUID productId,
        String customerName,
        String customerPhone,
        String address
) {
}
