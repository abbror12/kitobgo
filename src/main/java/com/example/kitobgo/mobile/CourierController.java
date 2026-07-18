package com.example.kitobgo.mobile;

import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.order.OrderQueryService;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Kuryer mobil ilovasi uchun endpointlar. Faqat COURIER roli kiradi
 * (xavfsizlik konfiguratsiyasida gate qilingan) va faqat o'ziga biriktirilgan
 * yetkazishlarni ko'radi.
 * <p>
 * Kuryerga buyurtmani admin/operator qo'lda biriktiradi
 * ({@code PATCH /api/orders/{id}/courier}). Statusni o'zgartirish umumiy endpoint
 * orqali: {@code PATCH /api/orders/{id}/status} (egalik OrderAuthorizationPolicy'da tekshiriladi).
 */
@RestController
@RequestMapping("/api/courier")
@RequiredArgsConstructor
public class CourierController {

    private final OrderQueryService queryService;

    /**
     * Kuryerga biriktirilgan yetkazishlar sahifasi; {@code ?status=} — ixtiyoriy filtr,
     * {@code ?page=&size=} — sahifalash (default 20, yangi buyurtmalar birinchi).
     */
    @GetMapping("/orders")
    public ResponseEntity<PagedResponse<OrderResponseDto>> myOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queryService.getMyCourierOrders(principal.user(), status, pageable));
    }

    /** Bitta yetkazish (faqat o'ziga biriktirilgan bo'lsa). */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponseDto> myOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queryService.getMyOrder(id, principal.user()));
    }
}
