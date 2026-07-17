package com.example.kitobgo.order;

/**
 * Buyurtma qaysi kanaldan kelgani.
 * <ul>
 *   <li>{@code WEBSITE} — sayt checkout; avtomatik round-robin taqsimotga tushadi;</li>
 *   <li>{@code SOCIAL_NETWORK} — SMM manager ijtimoiy tarmoq (Instagram, Telegram va h.k.)
 *       lead'idan qo'lda yaratgan; yaratgan xodimda qoladi va avto-taqsimotga tushmaydi.</li>
 * </ul>
 * Ilgari {@code INSTAGRAM} va {@code TELEGRAM} alohida qiymat edi, lekin ular kodda hech
 * qayerda farqlanmasdi — butun mantiq "WEBSITE mi yoki yo'qmi" degan shartga qurilgan.
 */
public enum OrderSource {
    WEBSITE, SOCIAL_NETWORK
}
