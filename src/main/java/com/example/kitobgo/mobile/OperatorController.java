package com.example.kitobgo.mobile;

import com.example.kitobgo.mobile.dto.OnlineStatusRequest;
import com.example.kitobgo.order.OrderService;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.security.UserPrincipal;
import com.example.kitobgo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final OperatorPresenceService presenceService;

    /**
     * Ishni boshlash/tugatish — operatorni online/offline qiladi.
     * online=true: hovuzdagi buyurtmalardan ish oladi; online=false: tegilmagan
     * buyurtmalari boshqa mavjud operatorlarga qayta taqsimlanadi.
     */
    @PatchMapping("/online")
    public ResponseEntity<UserResponse> setOnline(
            @Valid @RequestBody OnlineStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(presenceService.setOnline(principal.user().getId(), request.online()));
    }

    /**
     * Heartbeat — ilova muntazam (masalan har 30-60 soniyada) yuborib turadi.
     * Operatorni "tirik" saqlaydi; signal timeout'dan uzoq to'xtasa operator
     * avtomatik "mavjud emas" holatiga o'tadi.
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal UserPrincipal principal) {
        presenceService.heartbeat(principal.user().getId());
        return ResponseEntity.noContent().build();
    }

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
