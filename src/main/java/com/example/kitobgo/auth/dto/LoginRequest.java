package com.example.kitobgo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Telefon raqami bo'sh bo'lishi mumkin emas")
        String phone,

        @NotBlank(message = "Parol bo'sh bo'lishi mumkin emas")
        String password
) {
}
