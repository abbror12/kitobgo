package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        UUID operatorId,
        UUID courierId,
        UUID productId,
        String customerName,
        String customerPhone,
        String address,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getOperator() != null ? order.getOperator().getId() : null,
                order.getCourier() != null ? order.getCourier().getId() : null,
                order.getProduct() != null ? order.getProduct().getId() : null,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getAddress(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
