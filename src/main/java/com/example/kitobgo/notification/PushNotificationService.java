package com.example.kitobgo.notification;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderItem;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.user.User;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Push xabarni tarmoq orqali darhol yubormaydi: biznes tranzaksiyasi ichida outbox'ga
 * yozadi. Tranzaksiya rollback bo'lsa xabar ham yaratilmaydi; commit bo'lsa worker uni
 * FCM'ga yetkazadi. Shu sababli API request FCM javobini kutmaydi va xabar yo'qolmaydi.
 */
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushNotificationOutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    public void notifyNewOrder(User operator, Order order) {
        enqueue(operator, "Yangi buyurtma", orderSummary(order), "NEW_ORDER", order.getId());
    }

    public void notifyNewDelivery(User courier, Order order) {
        enqueue(courier, "Yangi yetkazma", orderSummary(order), "NEW_DELIVERY", order.getId());
    }

    public void notifyOrderUpdated(User target, Order order, OrderStatus newStatus) {
        enqueue(target, "Buyurtma yangilandi",
                order.getCustomerName() + " — " + statusLabel(newStatus),
                "ORDER_UPDATED", order.getId());
    }

    private void enqueue(User target, String title, String body, String type, UUID orderId) {
        if (target == null || target.getId() == null || orderId == null) {
            return;
        }
        outboxRepository.save(PushNotificationOutbox.builder()
                .userId(target.getId())
                .orderId(orderId)
                .type(type)
                .title(title)
                .body(body)
                .build());
        meterRegistry.counter("kitobgo.push.outbox.enqueued", "type", type).increment();
    }

    private String orderSummary(Order order) {
        int count = 0;
        long total = 0;
        for (OrderItem item : order.getItems()) {
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            count += quantity;
            if (item.getPriceAtPurchase() != null) {
                total += (long) item.getPriceAtPurchase() * quantity;
            }
        }
        return order.getCustomerName() + " — " + count + " ta kitob — " + formatPrice(total) + " so'm";
    }

    private String formatPrice(long price) {
        return String.format("%,d", price).replace(',', ' ');
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case NEW -> "Yangi";
            case CONFIRMED -> "Tasdiqlandi";
            case IN_DELIVERY -> "Yetkazilmoqda";
            case DELIVERED -> "Yetkazildi";
            case CANCELLED -> "Bekor qilindi";
            case RETURNED -> "Qaytib keldi";
            case REPROCESSING -> "Qayta ishlanmoqda";
        };
    }
}
