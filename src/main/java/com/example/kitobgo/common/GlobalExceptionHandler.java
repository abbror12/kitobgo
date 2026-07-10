package com.example.kitobgo.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    /** Noto'g'ri kirish ma'lumoti (masalan rasm bo'lmagan fayl) — 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    /**
     * Optimistik lock konflikti — bir yozuvni ikki so'rov bir vaqtda o'zgartirganda — 409.
     * (Masalan zaxira bir vaqtda kamaytirilganda.)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Ma'lumot boshqa so'rov tomonidan o'zgartirildi, iltimos qayta urinib ko'ring"));
    }
}
