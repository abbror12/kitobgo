package com.example.kitobgo.order.emu;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderItem;
import com.example.kitobgo.product.Product;

import java.util.Objects;
import java.util.Set;

/**
 * EMU pasilkasi nomi — buyurtma qatorlaridan avtomatik yasaladi (saqlanmaydi, har safar hisoblanadi).
 * <p>
 * Nom birinchi kitob nomidan va qolgan kitoblar sonidan tuziladi (jami miqdor bo'yicha,
 * ya'ni bitta kitobning 3 nusxasi ham "3 ta"):
 * <ul>
 *   <li>1 ta kitob — {@code "Alkimyogar"};</li>
 *   <li>3 ta kitob — {@code "Alkimyogar va yana 2 ta kitob"};</li>
 *   <li>uzun nom — oxiri kesilib {@code "..."} qo'yiladi.</li>
 * </ul>
 */
public final class EmuParcelName {

    /**
     * EMU maydoniga sig'adigan uzunlik — nom bundan uzun bo'lsa kesiladi.
     * EMU'ning haqiqiy chegarasi aniqlansa shu yerda o'zgartiriladi.
     */
    private static final int MAX_LENGTH = 100;

    private static final String ELLIPSIS = "...";

    /** Mahsulot yoki uning nomi yo'q bo'lganda ishlatiladigan zaxira nom. */
    private static final String FALLBACK = "Kitob";

    private EmuParcelName() {
    }

    /** Buyurtma uchun pasilka nomi. Qatorlar bo'sh bo'lsa — {@code "Kitob"}. */
    public static String of(Order order) {
        Set<OrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            return FALLBACK;
        }

        String firstTitle = titleOf(items.iterator().next());
        int totalQuantity = items.stream()
                .map(OrderItem::getQuantity)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

        if (totalQuantity <= 1) {
            return truncate(firstTitle, MAX_LENGTH);
        }

        String suffix = " va yana " + (totalQuantity - 1) + " ta kitob";
        return truncate(firstTitle, MAX_LENGTH - suffix.length()) + suffix;
    }

    private static String titleOf(OrderItem item) {
        Product product = item.getProduct();
        if (product == null || product.getTitle() == null || product.getTitle().isBlank()) {
            return FALLBACK;
        }
        return product.getTitle().trim();
    }

    /** Nomni {@code maxLength} ga sig'diradi; kesilса oxiriga "..." qo'yadi. */
    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        int keep = Math.max(0, maxLength - ELLIPSIS.length());
        return value.substring(0, keep).stripTrailing() + ELLIPSIS;
    }
}
