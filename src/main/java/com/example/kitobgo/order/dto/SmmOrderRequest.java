package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * SMM manager ijtimoiy tarmoq lead'idan qo'lda buyurtma yaratish so'rovi.
 * <p>
 * Saytdan farqli o'laroq bu yerda <b>hamma maydon majburiy</b>: SMM manager lead bilan
 * chatda gaplashib bo'lgan, ya'ni ism, telefon, viloyat, tuman va mo'ljal unda allaqachon
 * bor. Shu sababli buyurtma darhol {@code CONFIRMED} yaratiladi — keyin qo'ng'iroq qilib
 * manzilni to'ldiradigan operator bosqichi yo'q, demak yetkazishga yaroqli manzilni shu
 * yerda talab qilishdan boshqa iloj yo'q (qiyoslang: {@code OrderService.assertDeliverable}).
 * <p>
 * {@code source} yo'q — bu endpoint orqali kelgan buyurtma har doim {@code SOCIAL_NETWORK}.
 * {@code deliveryMethod} ham yo'q — marshrut hamma buyurtma uchun bir xil qoida bo'yicha
 * viloyatdan chiqadi. Pochta (EMU) ma'lumotlari ham so'ralmaydi: pasilkani admin eksport
 * qilganda yaratadi.
 */
public record SmmOrderRequest(
        @NotEmpty(message = "Buyurtmada kamida bitta mahsulot bo'lishi kerak")
        List<OrderItemRequest> items,

        @NotBlank(message = "Mijoz ismi ko'rsatilishi shart")
        String customerName,

        @NotBlank(message = "Telefon raqami ko'rsatilishi shart")
        String customerPhone,

        @NotNull(message = "Viloyat (region) ko'rsatilishi shart")
        Region region,

        @NotBlank(message = "Tuman ko'rsatilishi shart")
        String district,

        @NotBlank(message = "Mo'ljal (uy/bog'cha/maktab orientiri) ko'rsatilishi shart")
        String landmark
) {
}
