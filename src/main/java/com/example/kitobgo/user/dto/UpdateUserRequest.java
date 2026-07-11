package com.example.kitobgo.user.dto;

import com.example.kitobgo.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Foydalanuvchi asosiy maydonlarini yangilash (parolsiz). */
public record UpdateUserRequest(
        @NotBlank(message = "Ism bo'sh bo'lishi mumkin emas")
        String name,

        @NotBlank(message = "Telefon raqami bo'sh bo'lishi mumkin emas")
        String phone,

        @NotNull(message = "Rol ko'rsatilishi shart")
        Role role
) {
}
