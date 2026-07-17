package com.example.kitobgo.order.dto;

import com.example.kitobgo.order.DeliveryMethod;
import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderSource;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.order.OrderStatusChange;
import com.example.kitobgo.order.Region;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OrderResponseDto(
        UUID id,
        OrderAssignee operator,   // biriktirilgan operator (id + ism + telefon), yoki null
        OrderAssignee courier,    // biriktirilgan kuryer (id + ism + telefon), yoki null
        List<OrderItemResponse> items,
        Integer totalPrice,
        String customerName,
        String customerPhone,
        // Manzil uch maydondan: region (viloyat) + district (tuman) + landmark (mo'ljal).
        // Alohida "address" (ko'cha/uy) maydoni yo'q — qarang Order dagi "Yetkazish manzili" izohi.
        String district,         // tuman
        String landmark,         // mo'ljal — uy/bog'cha/maktab orientiri
        OrderStatus status,
        OrderSource source,           // buyurtma kanali: WEBSITE | SOCIAL_NETWORK
        Region region,                // yetkazish viloyati (yoki null — eski buyurtmalar)
        DeliveryMethod deliveryMethod, // COURIER | EMU (viloyatдан kelib chiqadi)
        EmuInfo emu,                  // EMU tomoni: pasilka nomi, trek-raqam, eksport vaqti (COURIER uchun null)
        LocalDateTime createdAt,
        // Status o'zgarishlari — eng eskidan yangiga, kim qilgani bilan. To'liq haqiqat shu yerda.
        List<OrderStatusChangeResponse> history,
        // Quyidagi 6 maydon tarixdan hisoblanadi (har statusning ENG OXIRGI vaqti) —
        // ilgari ular orders jadvalidagi ustunlar edi va aynan shunday, ustiga yozilib
        // ishlardi. Mavjud mijozlar (admin panel) buzilmasligi uchun saqlab qolindi;
        // takroriy o'tishlar kerak bo'lsa history'ga qarang.
        LocalDateTime confirmedAt,
        LocalDateTime inDeliveryAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        LocalDateTime returnedAt,
        LocalDateTime reprocessingAt
) {
    public static OrderResponseDto from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        Integer totalPrice = items.stream()
                .map(OrderItemResponse::lineTotal)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);

        List<OrderStatusChangeResponse> history = order.getHistory().stream()
                .sorted(Comparator.comparing(OrderStatusChange::getChangedAt))
                .map(OrderStatusChangeResponse::from)
                .toList();

        return new OrderResponseDto(
                order.getId(),
                OrderAssignee.from(order.getOperator()),
                OrderAssignee.from(order.getCourier()),
                items,
                totalPrice,
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDistrict(),
                order.getLandmark(),
                order.getStatus(),
                order.getSource(),
                order.getRegion(),
                order.getDeliveryMethod(),
                EmuInfo.from(order),
                order.getCreatedAt(),
                history,
                lastTimeOf(history, OrderStatus.CONFIRMED),
                lastTimeOf(history, OrderStatus.IN_DELIVERY),
                lastTimeOf(history, OrderStatus.DELIVERED),
                lastTimeOf(history, OrderStatus.CANCELLED),
                lastTimeOf(history, OrderStatus.RETURNED),
                lastTimeOf(history, OrderStatus.REPROCESSING)
        );
    }

    /**
     * Berilgan statusga oxirgi marta o'tilgan vaqt (yoki null — hech qachon o'tilmagan).
     * Tarix o'sish tartibida bo'lgani uchun oxirgi mos qator eng yangisi — bu eski
     * ustunlar mantig'ini aynan takrorlaydi (har o'tish oldingisining ustiga yozardi).
     */
    private static LocalDateTime lastTimeOf(List<OrderStatusChangeResponse> history, OrderStatus status) {
        LocalDateTime found = null;
        for (OrderStatusChangeResponse change : history) {
            if (change.status() == status) {
                found = change.changedAt();
            }
        }
        return found;
    }
}
