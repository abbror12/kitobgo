package com.example.kitobgo.mobile;

import com.example.kitobgo.order.OrderService;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Kuryer mobil ilovasi uchun endpointlar. Faqat COURIER roli kiradi
 * (xavfsizlik konfiguratsiyasida gate qilingan) va faqat o'ziga biriktirilgan
 * yetkazishlarni ko'radi.
 * <p>
 * Statusni o'zgartirish umumiy endpoint orqali: {@code PATCH /api/orders/{id}/status}
 * (egalik OrderService'da tekshiriladi).
 */
@RestController
@RequestMapping("/api/courier")
@RequiredArgsConstructor
public class CourierController {

    private final OrderService orderService;

    /** Kuryerga biriktirilgan yetkazishlar ro'yxati. */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> myOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyCourierOrders(principal.user()));
    }

    /** Bitta yetkazish (faqat o'ziga biriktirilgan bo'lsa). */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponseDto> myOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyOrder(id, principal.user()));
    }
}
