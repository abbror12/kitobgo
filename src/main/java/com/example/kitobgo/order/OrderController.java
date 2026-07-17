package com.example.kitobgo.order;

import com.example.kitobgo.order.dto.AssignCourierRequest;
import com.example.kitobgo.order.dto.ChangeStatusRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.dto.OrderResponseDto;
import com.example.kitobgo.order.dto.RegionResponse;
import com.example.kitobgo.order.dto.TrackingRequest;
import com.example.kitobgo.order.dto.UpdateDeliveryRequest;
import com.example.kitobgo.order.emu.EmuService;
import com.example.kitobgo.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final EmuService emuService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@Valid @RequestBody OrderRequestDto requestDto) {
        OrderResponseDto response = orderService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Checkout uchun viloyatlar ro'yxati (har biriga yetkazish turi bilan). Ochiq. */
    @GetMapping("/regions")
    public ResponseEntity<List<RegionResponse>> regions() {
        return ResponseEntity.ok(orderService.regions());
    }

    /**
     * Buyurtma statusini o'zgartiradi.
     * ADMIN/SUPER_ADMIN — har qanday buyurtmani; OPERATOR/COURIER — faqat o'ziniki.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.changeStatus(id, request.status(), principal.user()));
    }

    /**
     * Buyurtmaga kuryer biriktiradi. Faqat ADMIN/SUPER_ADMIN.
     */
    @PatchMapping("/{id}/courier")
    public ResponseEntity<OrderResponseDto> assignCourier(
            @PathVariable UUID id,
            @Valid @RequestBody AssignCourierRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.assignCourier(id, request.courierId(), principal.user()));
    }

    /** Buyurtmadan kuryerni yechadi. Faqat ADMIN/SUPER_ADMIN. */
    @DeleteMapping("/{id}/courier")
    public ResponseEntity<OrderResponseDto> unassignCourier(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.unassignCourier(id, principal.user()));
    }

    /**
     * Yetkazish manzilini yangilaydi (operator mijoz bilan gaplashib to'ldiradi):
     * tuman, mo'ljal va ixtiyoriy viloyat. ADMIN/SUPER_ADMIN har qanday;
     * OPERATOR faqat o'z buyurtmasi. SMM manager manzilni bu yerda emas, lead
     * yaratishda kiritadi — unga bu endpoint yopiq.
     */
    @PatchMapping("/{id}/address")
    public ResponseEntity<OrderResponseDto> updateDelivery(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.updateDelivery(id, request, principal.user()));
    }

    /**
     * EMU bergan trek-raqamni pasilkaga yozadi. Faqat ADMIN/SUPER_ADMIN va faqat
     * EMU ga allaqachon eksport qilingan buyurtmalar uchun.
     */
    @PatchMapping("/{id}/tracking")
    public ResponseEntity<OrderResponseDto> setTracking(
            @PathVariable UUID id,
            @Valid @RequestBody TrackingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(emuService.setTrackingNumber(id, request.trackingNumber(), principal.user()));
    }

    /**
     * Marshrutsiz buyurtmalar — tasdiqlangan, lekin yetkazish turi tanlanmagan
     * (Toshkent viloyati). Admin har biriga EMU yoki kuryerni belgilashi kerak.
     * Faqat ADMIN/SUPER_ADMIN.
     */
    @GetMapping("/unrouted")
    public ResponseEntity<List<OrderResponseDto>> unrouted(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.unrouted(principal.user()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    /**
     * Buyurtmalar ro'yxati (admin panel). Ixtiyoriy filtrlar:
     * {@code ?operatorId=} — bitta operatorning buyurtmalari,
     * {@code ?status=} — ma'lum statusdagilar; birga ishlatish ham mumkin.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAll(
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getAll(operatorId, status));
    }
}
