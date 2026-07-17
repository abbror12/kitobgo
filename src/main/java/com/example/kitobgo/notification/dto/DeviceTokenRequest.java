package com.example.kitobgo.notification.dto;

import jakarta.validation.constraints.NotBlank;

/** Qurilma token'ini ro'yxatga olish/o'chirish so'rovi (mobil ilova). */
public record DeviceTokenRequest(
        @NotBlank(message = "token bo'sh bo'lishi mumkin emas")
        String token,
        String platform
) {
}
