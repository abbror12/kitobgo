package com.example.kitobgo.smm;

import com.example.kitobgo.common.PagedResponse;
import com.example.kitobgo.order.OrderCreationService;
import com.example.kitobgo.order.OrderQueryService;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.order.dto.SmmOrderRequest;
import com.example.kitobgo.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * SMM manager web ilovasi uchun endpointlar. Faqat SMM_MANAGER roli kiradi
 * (xavfsizlik konfiguratsiyasida gate qilingan).
 * <p>
 * Shu sababli alohida paketda, {@code mobile} ichida emas: kuryer va operator mobil
 * ilovadan ishlaydi, SMM manager esa brauzerdan — u lead bilan kompyuterda,
 * Instagram/Telegram chatining yonida ishlaydi.
 * <p>
 * SMM manager — lead kiritish nuqtasi, boshqaruv roli emas: lead'dan to'liq ma'lumot bilan
 * buyurtma yaratadi (u darhol {@code CONFIRMED} bo'ladi) va o'zi yaratganlarini ko'radi —
 * boshqa hech nima. Statusni o'zgartirish, manzilni tahrirlash va kuryer biriktirish unga
 * berilmagan; buyurtma yaratilgandan keyin operator/admin qo'liga o'tadi. Bu cheklov
 * {@code OrderAuthorizationPolicy}'da (egalik + rol tekshiruvi) amalga oshirilgan.
 */
@RestController
@RequestMapping("/api/smm")
@RequiredArgsConstructor
public class SmmController {

    private final OrderCreationService creationService;
    private final OrderQueryService queryService;

    /** Instagram/Telegram lead'idan qo'lda buyurtma yaratadi (o'ziga biriktiriladi). */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponseDto> create(
            @Valid @RequestBody SmmOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creationService.createBySmm(request, principal.user()));
    }

    /**
     * SMM manager o'zi yaratgan buyurtmalar sahifasi; {@code ?status=} — ixtiyoriy filtr,
     * {@code ?page=&size=} — sahifalash (default 20, yangi buyurtmalar birinchi).
     */
    @GetMapping("/orders")
    public ResponseEntity<PagedResponse<OrderResponseDto>> myOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queryService.getMyOwnedOrders(principal.user(), status, pageable));
    }

    /** Bitta buyurtma (faqat o'zi yaratgan bo'lsa). */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponseDto> myOrder(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(queryService.getMyOrder(id, principal.user()));
    }
}
