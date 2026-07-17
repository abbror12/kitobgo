package com.example.kitobgo.notification;

import com.example.kitobgo.notification.dto.DeviceTokenRequest;
import com.example.kitobgo.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobil ilova FCM token'larini ro'yxatga oladigan endpointlar.
 * Ikkalasi ham JWT bilan himoyalangan; foydalanuvchi token'dan aniqlanadi.
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    /** Token'ni ro'yxatga olish (upsert, idempotent) — har login/ilova ochilishida chaqiriladi. */
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody DeviceTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        deviceTokenService.register(principal.user(), request.token(), request.platform());
        return ResponseEntity.ok().build();
    }

    /** Token'ni o'chirish — logout paytida chaqiriladi. */
    @PostMapping("/unregister")
    public ResponseEntity<Void> unregister(
            @Valid @RequestBody DeviceTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        deviceTokenService.unregister(principal.user(), request.token());
        return ResponseEntity.ok().build();
    }
}
