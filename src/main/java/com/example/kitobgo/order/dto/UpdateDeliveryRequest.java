package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.DeliveryMethod;
import com.example.kitobgo.order.Region;
import jakarta.validation.constraints.NotBlank;

/**
 * Operator mijoz bilan gaplashib yetkazish manzilini yozadi: tuman va mo'ljal.
 * Ko'cha/uy uchun alohida maydon yo'q — mo'ljal ({@code landmark}) yagona tafsilot
 * (qarang {@code Order} dagi "Yetkazish manzili" izohi).
 * <p>
 * {@code region} ixtiyoriy — berilsa va o'zgargan bo'lsa viloyat yangilanadi.
 * {@code deliveryMethod} ixtiyoriy va faqat <b>tasdiqlangan</b> buyurtmada ishlaydi —
 * Toshkent viloyati kabi ikkala tur mumkin bo'lganda admin tanlashi uchun.
 */
public record UpdateDeliveryRequest(
        @NotBlank(message = "Tuman bo'sh bo'lishi mumkin emas")
        String district,

        @NotBlank(message = "Mo'ljal bo'sh bo'lishi mumkin emas")
        String landmark,

        Region region,
        DeliveryMethod deliveryMethod
) {
}
