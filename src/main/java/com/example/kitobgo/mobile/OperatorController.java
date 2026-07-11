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
 * Operator mobil ilovasi uchun endpointlar. Faqat OPERATOR roli kiradi
 * (xavfsizlik konfiguratsiyasida gate qilingan) va faqat o'ziga biriktirilgan
 * buyurtmalarni ko'radi.
 * <p>
 * Statusni o'zgartirish va kuryer biriktirish umumiy endpointlar orqali amalga
 * oshiriladi: {@code PATCH /api/orders/{id}/status}, {@code PATCH /api/orders/{id}/courier}
 * (egalik OrderService'da tekshiriladi).
 */
@RestController
@RequestMapping("/api/operator")
@RequiredArgsConstructor
public class OperatorController {

    private final OrderService orderService;

    /** Operatorga biriktirilgan buyurtmalar ro'yxati. */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> myOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyOperatorOrders(principal.user()));
    }

    /** Bitta buyurtma (faqat o'ziga biriktirilgan bo'lsa). */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponseDto> myOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getMyOrder(id, principal.user()));
    }
}
