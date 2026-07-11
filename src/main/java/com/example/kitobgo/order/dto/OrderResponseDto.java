package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        OrderAssignee operator,   // biriktirilgan operator (id + ism + telefon), yoki null
        OrderAssignee courier,    // biriktirilgan kuryer (id + ism + telefon), yoki null
        List<OrderItemResponse> items,
        Integer totalPrice,
        String customerName,
        String customerPhone,
        String address,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        LocalDateTime inDeliveryAt,
        LocalDateTime deliveredAt,
        LocalDateTime returnedAt
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
                OrderAssignee.from(order.getOperator()),
                OrderAssignee.from(order.getCourier()),
                items,
                totalPrice,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getAddress(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getInDeliveryAt(),
                order.getDeliveredAt(),
                order.getReturnedAt()
        );
    }
}
