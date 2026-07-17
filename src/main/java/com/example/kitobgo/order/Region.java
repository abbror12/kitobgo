package com.example.kitobgo.order;

import java.util.List;

/**
 * O'zbekiston hududlari. Har viloyat qaysi yetkazish turlarини qo'llab-quvvatlashini biladi:
 * <ul>
 *   <li>Toshkent shahri — faqat kuryer;</li>
 *   <li>Toshkent viloyati — EMU <b>va</b> kuryer (ikkalasi ham mumkin, standart EMU —
 *       saytdan avtomatik EMU, adminning o'zi kerak bo'lsa kuryerga o'tkazadi);</li>
 *   <li>qolgan viloyatlar — faqat EMU (pochta).</li>
 * </ul>
 * Marshrut buyurtma tasdiqlanganda shu jadvaldan avtomatik chiqadi ({@link #autoRoute()}):
 * bitta tur mumkin bo'lsa — o'sha qo'yiladi; bir nechta bo'lsa (Toshkent viloyati) —
 * qaror adminga qoladi.
 */
public enum Region {
    TASHKENT_CITY("Toshkent shahri", List.of(DeliveryMethod.COURIER)),
    TASHKENT_REGION("Toshkent viloyati", List.of(DeliveryMethod.EMU, DeliveryMethod.COURIER)),
    ANDIJAN("Andijon viloyati", List.of(DeliveryMethod.EMU)),
    FERGANA("Farg'ona viloyati", List.of(DeliveryMethod.EMU)),
    NAMANGAN("Namangan viloyati", List.of(DeliveryMethod.EMU)),
    SIRDARYO("Sirdaryo viloyati", List.of(DeliveryMethod.EMU)),
    JIZZAKH("Jizzax viloyati", List.of(DeliveryMethod.EMU)),
    SAMARKAND("Samarqand viloyati", List.of(DeliveryMethod.EMU)),
    KASHKADARYO("Qashqadaryo viloyati", List.of(DeliveryMethod.EMU)),
    SURKHANDARYO("Surxondaryo viloyati", List.of(DeliveryMethod.EMU)),
    BUKHARA("Buxoro viloyati", List.of(DeliveryMethod.EMU)),
    NAVOI("Navoiy viloyati", List.of(DeliveryMethod.EMU)),
    KHOREZM("Xorazm viloyati", List.of(DeliveryMethod.EMU)),
    KARAKALPAKSTAN("Qoraqalpog'iston Respublikasi", List.of(DeliveryMethod.EMU));

    private final String label;
    private final List<DeliveryMethod> allowedMethods;

    Region(String label, List<DeliveryMethod> allowedMethods) {
        this.label = label;
        this.allowedMethods = allowedMethods;
    }

    /** O'zbek tilidagi ko'rsatiladigan nom (checkout dropdown uchun). */
    public String getLabel() {
        return label;
    }

    /** Shu viloyat uchun mumkin bo'lgan yetkazish turlari (definitsiya tartibida). */
    public List<DeliveryMethod> allowedMethods() {
        return allowedMethods;
    }

    /** Berilgan yetkazish turi shu viloyat uchun mumkinmi. */
    public boolean allows(DeliveryMethod method) {
        return allowedMethods.contains(method);
    }

    /**
     * Buyurtma tasdiqlanganda avtomatik qo'yiladigan yetkazish turi, yoki {@code null} —
     * tanlash adminga qolsa.
     * <p>
     * Qoida sodda: tanlov yo'q bo'lsa (bitta tur mumkin) tizim o'zi qo'yadi — Toshkent
     * shahri kuryerga, viloyatlar EMU'ga. Haqiqiy tanlov bor bo'lsa (Toshkent viloyati:
     * EMU ham, kuryer ham) tizim taxmin qilmaydi — admin hal qiladi.
     */
    public DeliveryMethod autoRoute() {
        return allowedMethods.size() == 1 ? allowedMethods.get(0) : null;
    }
}
