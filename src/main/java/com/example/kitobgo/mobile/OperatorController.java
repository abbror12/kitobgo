package com.example.kitobgo.mobile;

import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.mobile.dto.OnlineStatusRequest;
import com.example.kitobgo.order.OrderQueryService;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.presence.PresenceService;
import com.example.kitobgo.security.UserPrincipal;
import com.example.kitobgo.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Operator mobil ilovasi uchun endpointlar. Faqat OPERATOR roli kiradi
 * (xavfsizlik konfiguratsiyasida gate qilingan) va faqat o'ziga biriktirilgan
 * buyurtmalarni ko'radi.
 * <p>
 * Statusni o'zgartirish umumiy endpoint orqali amalga oshiriladi:
 * {@code PATCH /api/orders/{id}/status} (egalik OrderAuthorizationPolicy'da tekshiriladi).
 * Kuryer biriktirish operatorga berilmagan — faqat admin qiladi.
 */
@RestController
@RequestMapping("/api/operator")
@RequiredArgsConstructor
public class OperatorController {

    private final OrderQueryService queryService;
    private final PresenceService presenceService;

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

    /**
     * Operatorga biriktirilgan buyurtmalar sahifasi; {@code ?status=} — ixtiyoriy filtr,
     * {@code ?page=&size=} — sahifalash (default 20, yangi buyurtmalar birinchi).
     */
    @GetMapping("/orders")
    public ResponseEntity<PagedResponse<OrderResponseDto>> myOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queryService.getMyOwnedOrders(principal.user(), status, pageable));
    }

    /** Bitta buyurtma (faqat o'ziga biriktirilgan bo'lsa). */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponseDto> myOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queryService.getMyOrder(id, principal.user()));
    }
}
