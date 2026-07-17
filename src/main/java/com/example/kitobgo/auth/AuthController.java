package com.example.kitobgo.auth;

import com.example.kitobgo.auth.dto.AuthResponse;
import com.example.kitobgo.auth.dto.LoginRequest;
import com.example.kitobgo.auth.dto.RefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Login — telefon + parol, muvaffaqiyatli bo'lsa access + refresh token qaytaradi. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Access token muddati tugaganda refresh token evaziga yangi juftlik olish.
     * Eski refresh token bekor bo'ladi (rotation) — javobdagi yangisini saqlang.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /** Logout — refresh tokenni bekor qiladi (shu qurilmadan chiqish). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
