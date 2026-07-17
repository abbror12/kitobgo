package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.order.OrderStatusChange;

import java.time.LocalDateTime;

/**
 * Status tarixining bitta qatori. {@code changedBy} null bo'lishi mumkin — sayt
 * checkout'i (mijoz o'zi yaratgan) yoki eski, migratsiyada ko'chirilgan yozuv.
 */
public record OrderStatusChangeResponse(
        OrderStatus status,
        LocalDateTime changedAt,
        OrderAssignee changedBy
) {
    public static OrderStatusChangeResponse from(OrderStatusChange change) {
        return new OrderStatusChangeResponse(
                change.getStatus(),
                change.getChangedAt(),
                OrderAssignee.from(change.getChangedBy()));
    }
}
