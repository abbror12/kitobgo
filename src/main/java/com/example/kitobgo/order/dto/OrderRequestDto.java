package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.Region;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
        @NotEmpty(message = "Buyurtmada kamida bitta mahsulot bo'lishi kerak")
        List<@Valid OrderItemRequest> items,

        @NotBlank(message = "Mijoz ismi ko'rsatilishi shart")
        String customerName,

        @NotBlank(message = "Telefon raqami ko'rsatilishi shart")
        String customerPhone,

        @NotNull(message = "Viloyat (region) ko'rsatilishi shart")
        Region region
) {
}
