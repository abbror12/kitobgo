package com.example.kitobgo.order.dto;

import java.util.List;

public record OrderRequestDto(
        List<OrderItemRequest> items,
        String customerName,
        String customerPhone,
        String address
) {
}
