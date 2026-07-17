package com.example.kitobgo.auth.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType
) {
    public static AuthResponse bearer(String token, String refreshToken) {
        return new AuthResponse(token, refreshToken, "Bearer");
    }
}
