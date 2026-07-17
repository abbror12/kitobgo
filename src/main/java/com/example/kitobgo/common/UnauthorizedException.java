package com.example.kitobgo.common;

/** Autentifikatsiya yaroqsiz (masalan refresh token noto'g'ri/muddati o'tgan) — 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
