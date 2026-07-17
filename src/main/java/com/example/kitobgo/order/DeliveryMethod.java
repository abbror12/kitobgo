package com.example.kitobgo.order;

/**
 * Buyurtma qanday yetkaziladi. {@code COURIER} — kuryer (Toshkent shahri va viloyati);
 * {@code EMU} — pochta/EMU orqali (qolgan viloyatlar). Buyurtma {@link Region}'idan
 * kelib chiqib avtomatik aniqlanadi.
 */
public enum DeliveryMethod {
    COURIER, EMU
}
