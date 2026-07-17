package com.example.kitobgo.order.dto;


public record OrderItemRequest(
        Long productId,
        Integer quantity
) {
}
