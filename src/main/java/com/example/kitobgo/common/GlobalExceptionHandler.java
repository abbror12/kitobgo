package com.example.kitobgo.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Barcha controllerlar uchun markazlashgan xatolik ishlovchisi — xatolarni
 * bir xil, tushunarli JSON ko'rinishida qaytaradi.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @Valid validatsiyasi buzilganda — qaysi maydon nega noto'g'ri ekanini qaytaradi. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    /** Login parol/telefon noto'g'ri bo'lganda — 401. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Telefon raqami yoki parol noto'g'ri"));
    }
}
