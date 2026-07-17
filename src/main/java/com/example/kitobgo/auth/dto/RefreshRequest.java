package com.example.kitobgo.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token bo'sh bo'lishi mumkin emas")
        String refreshToken
) {
}
