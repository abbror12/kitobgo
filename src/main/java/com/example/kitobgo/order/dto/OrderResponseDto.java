package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        UUID operatorId,
        UUID courierId,
        List<OrderItemResponse> items,
        Integer totalPrice,
        String customerName,
        String customerPhone,
        String address,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponseDto from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        Integer totalPrice = items.stream()
                .map(OrderItemResponse::lineTotal)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

        return new OrderResponseDto(
                order.getId(),
                order.getOperator() != null ? order.getOperator().getId() : null,
                order.getCourier() != null ? order.getCourier().getId() : null,
                items,
                totalPrice,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getAddress(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
