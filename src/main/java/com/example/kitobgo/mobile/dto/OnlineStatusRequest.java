package com.example.kitobgo.mobile.dto;

import jakarta.validation.constraints.NotNull;

/** Operator online holatini o'zgartirish (ishni boshlash/tugatish). */
public record OnlineStatusRequest(
        @NotNull(message = "online maydoni ko'rsatilishi shart")
        Boolean online
) {
}
