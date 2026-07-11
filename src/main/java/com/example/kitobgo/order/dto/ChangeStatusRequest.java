package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

/** Buyurtma statusini o'zgartirish so'rovi. */
public record ChangeStatusRequest(
        @NotNull(message = "Status ko'rsatilishi shart")
        OrderStatus status
) {
}
