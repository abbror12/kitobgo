package com.example.kitobgo.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Foydalanuvchi autentifikatsiyadan o'tgan, lekin aynan shu amalga huquqi yo'q — 403. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
