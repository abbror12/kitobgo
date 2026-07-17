package com.example.kitobgo.order;

import com.example.kitobgo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Buyurtma statusining bitta o'zgarishi — kim, qachon, qaysi statusga o'tkazgani.
 * Yozuvlar faqat <b>qo'shiladi</b>, hech qachon ustiga yozilmaydi yoki o'chirilmaydi.
 * <p>
 * Ilgari bu ma'lumot {@code orders} jadvalidagi 6 ta vaqt ustunida edi (confirmed_at,
 * returned_at, ...). U yerda har o'tish o'z ustuniga yozilgani uchun takroriy o'tishlar
 * bir-birini o'chirar edi: kuryer qaytarib, operator qayta ishlab, yana qaytarilsa
 * birinchi qaytish vaqti yo'qolardi. Kim o'zgartirgani esa umuman saqlanmasdi.
 */
@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /**
     * O'zgartirgan xodim. {@code null} — sayt checkout'i (mijozning o'zi yaratgan,
     * tizimda foydalanuvchisi yo'q) yoki migratsiyada ko'chirilgan eski yozuv
     * (u paytda kim qilgani saqlanmagan).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;
}
