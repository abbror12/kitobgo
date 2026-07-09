package com.example.kitobgo.user.dto;

import com.example.kitobgo.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Ism bo'sh bo'lishi mumkin emas")
        String name,

        @NotBlank(message = "Telefon raqami bo'sh bo'lishi mumkin emas")
        String phone,

        @NotBlank(message = "Parol bo'sh bo'lishi mumkin emas")
        @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
        String password,

        @NotNull(message = "Rol ko'rsatilishi shart")
        Role role
) {
}
