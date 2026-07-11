package com.example.kitobgo.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin tomonidan foydalanuvchi parolini qayta o'rnatish. */
public record ChangePasswordRequest(
        @NotBlank(message = "Parol bo'sh bo'lishi mumkin emas")
        @Size(min = 6, message = "Parol kamida 6 ta belgidan iborat bo'lishi kerak")
        String password
) {
}
