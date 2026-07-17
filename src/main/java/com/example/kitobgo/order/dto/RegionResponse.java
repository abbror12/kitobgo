package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.DeliveryMethod;
import com.example.kitobgo.order.Region;

import java.util.List;

/**
 * Viloyatlar ro'yxati — operator/admin panelida viloyat tanlash uchun (checkout'da
 * mijozdan viloyat so'ralmaydi).
 * <p>
 * {@code autoRoute} — buyurtma tasdiqlanganda avtomatik qo'yiladigan yetkazish turi,
 * yoki {@code null} bo'lsa admin {@code deliveryMethods} ichidan o'zi tanlashi kerak
 * (Toshkent viloyati). Panel shunga qarab tanlov ko'rsatadi.
 */
public record RegionResponse(
        String code,               // enum nomi (so'rovda yuboriladigan qiymat)
        String label,              // o'zbekcha nom
        List<DeliveryMethod> deliveryMethods,
        DeliveryMethod autoRoute
) {
    public static RegionResponse from(Region region) {
        return new RegionResponse(
                region.name(), region.getLabel(), region.allowedMethods(), region.autoRoute());
    }
}
