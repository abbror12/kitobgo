package com.example.kitobgo.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Buyurtmaga kuryer biriktirish so'rovi. */
public record AssignCourierRequest(
        @NotNull(message = "Kuryer id ko'rsatilishi shart")
        UUID courierId
) {
}
