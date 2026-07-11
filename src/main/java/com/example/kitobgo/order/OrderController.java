package com.example.kitobgo.order;

import com.example.kitobgo.order.dto.AssignCourierRequest;
import com.example.kitobgo.order.dto.ChangeStatusRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;
import com.example.kitobgo.order.dto.OrderResponseDto;
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

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@RequestBody OrderRequestDto requestDto) {
        OrderResponseDto response = orderService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * Buyurtmaga kuryer biriktiradi.
     * ADMIN/SUPER_ADMIN — har qanday buyurtmaga; biriktirilgan OPERATOR — o'z buyurtmasiga.
     */
    @PatchMapping("/{id}/courier")
    public ResponseEntity<OrderResponseDto> assignCourier(
            @PathVariable UUID id,
            @Valid @RequestBody AssignCourierRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.assignCourier(id, request.courierId(), principal.user()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }
}
