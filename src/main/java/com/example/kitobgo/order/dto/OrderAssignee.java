package com.example.kitobgo.order.dto;

import com.example.kitobgo.user.User;

import java.util.UUID;

/** Buyurtmaga biriktirilgan xodim (operator yoki kuryer) — qisqacha ma'lumot. */
public record OrderAssignee(
        UUID id,
        String name,
        String phone
) {
    /** {@code null} foydalanuvchi uchun {@code null} qaytaradi (biriktirilmagan). */
    public static OrderAssignee from(User user) {
        return user == null ? null : new OrderAssignee(user.getId(), user.getName(), user.getPhone());
    }
}
