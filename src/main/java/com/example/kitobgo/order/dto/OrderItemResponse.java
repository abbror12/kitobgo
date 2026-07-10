package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.OrderItem;

import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String productTitle,
        Integer quantity,
        Integer priceAtPurchase,
        Integer lineTotal
) {
    public static OrderItemResponse from(OrderItem item) {
        Integer price = item.getPriceAtPurchase();
        Integer qty = item.getQuantity();
        Integer lineTotal = (price != null && qty != null) ? price * qty : null;
        return new OrderItemResponse(
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProduct() != null ? item.getProduct().getTitle() : null,
                qty,
                price,
                lineTotal
        );
    }
}
