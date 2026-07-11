package com.example.kitobgo.user.dto;

import com.example.kitobgo.user.Role;
import jakarta.validation.constraints.NotNull;

/** Foydalanuvchi rolini o'zgartirish. */
public record ChangeRoleRequest(
        @NotNull(message = "Rol ko'rsatilishi shart")
        Role role
) {
}
