package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.Region;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Sayt (checkout) buyurtmasi — mijozdan minimal ma'lumot.
 * <p>
 * Manzil <b>so'ralmaydi</b>: u erkin matn bo'lgani uchun baribir ishonchsiz, operator esa
 * mijozga qo'ng'iroq qilib to'liq manzilni oladi ({@code PATCH /api/orders/{id}/address}).
 * Viloyat esa ro'yxatdan tanlanadi ({@code GET /api/orders/regions}), ya'ni noto'g'ri
 * qiymat kelmaydi.
 * <p>
 * Yetkazish turi ham so'ralmaydi — u buyurtma tasdiqlanganda viloyatdan avtomatik
 * chiqadi ({@code Region.autoRoute()}).
 */
public record OrderRequestDto(
        List<OrderItemRequest> items,
        String customerName,
        String customerPhone,

        @NotNull(message = "Viloyat (region) ko'rsatilishi shart")
        Region region
) {
}
