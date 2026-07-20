package com.example.kitobgo.notification;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderItem;
import com.example.kitobgo.order.OrderStatus;
import com.example.kitobgo.user.User;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Buyurtma hodisalari haqida operator/kuryer telefoniga FCM push yuboradi.
 * <p>
 * Xabar formati ilova bilan kelishilgan shartnomaga mos (docs/BACKEND_FCM_SPEC.md):
 * {@code notification} + {@code data} bloklari, {@code android.priority=high},
 * {@code channel_id=orders}. Yuborish tranzaksiya commit'idan KEYIN amalga oshadi —
 * ilova push kelgan zahoti ro'yxatni qayta yuklaganda buyurtma bazada allaqachon
 * ko'rinadi. Push yuborishdagi xato biznes oqimini hech qachon buzmaydi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    /** Android ilovada oldindan yaratilgan notification kanali. */
    private static final String CHANNEL_ID = "orders";

    private final DeviceTokenRepository deviceTokenRepository;

    /** Yangi buyurtma yaratildi yoki operatorga biriktirildi. */
    public void notifyNewOrder(User operator, Order order) {
        send(operator, "Yangi buyurtma", orderSummary(order), "NEW_ORDER", order.getId());
    }

    /** Buyurtma kuryerga biriktirildi (yetkazishga tayyor). */
    public void notifyNewDelivery(User courier, Order order) {
        send(courier, "Yangi yetkazma", orderSummary(order), "NEW_DELIVERY", order.getId());
    }

    /** Buyurtma holati o'zgardi — aloqador operator/kuryerga. */
    public void notifyOrderUpdated(User target, Order order, OrderStatus newStatus) {
        send(target, "Buyurtma yangilandi",
                order.getCustomerName() + " — " + statusLabel(newStatus),
                "ORDER_UPDATED", order.getId());
    }

    /**
     * Xabar matnini hozir (ochiq Hibernate session ichida — lazy items o'qish uchun)
     * tayyorlab, tarmoq orqali yuborishni commit'dan keyinga qoldiradi.
     */
    private void send(User target, String title, String body, String type, UUID orderId) {
        if (target == null || orderId == null) {
            return;
        }
        UUID userId = target.getId();
        String orderIdStr = orderId.toString();
        runAfterCommit(() -> doSend(userId, title, body, type, orderIdStr));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void doSend(UUID userId, String title, String body, String type, String orderId) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.debug("FCM sozlanmagan — push yuborilmadi (type={}, orderId={})", type, orderId);
                return;
            }
            List<String> tokens = deviceTokenRepository.findTokensByUserId(userId);
            if (tokens.isEmpty()) {
                return;
            }

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", type)
                    .putData("orderId", orderId)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId(CHANNEL_ID)
                                    .build())
                            .build())
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            cleanupInvalidTokens(tokens, response);
            log.info("FCM push yuborildi: type={}, orderId={}, muvaffaqiyatli={}/{}",
                    type, orderId, response.getSuccessCount(), tokens.size());
        } catch (Exception e) {
            log.error("FCM push yuborishda xato (type={}, orderId={})", type, orderId, e);
        }
    }

    /** Eskirgan/yaroqsiz token'larni bazadan o'chiradi (spec 6-bo'lim — majburiy). */
    private void cleanupInvalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalid = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse r = responses.get(i);
            if (!r.isSuccessful() && r.getException() != null) {
                MessagingErrorCode code = r.getException().getMessagingErrorCode();
                // Xato sababi yozilmasa nosozlikni topib bo'lmaydi (token o'chirilmasa ham).
                log.warn("FCM token rad etildi: kod={}, xabar={}, token=...{}",
                        code, r.getException().getMessage(), tail(tokens.get(i)));
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    invalid.add(tokens.get(i));
                }
            }
        }
        if (!invalid.isEmpty()) {
            deviceTokenRepository.deleteByTokenIn(invalid);
            log.info("{} ta yaroqsiz FCM token o'chirildi", invalid.size());
        }
    }

    /** Token'ning oxirgi 8 belgisi — logda to'liq token yozilmasligi uchun. */
    private String tail(String token) {
        return token.length() <= 8 ? token : token.substring(token.length() - 8);
    }

    /** "Ali Valiyev — 2 ta kitob — 150 000 so'm" ko'rinishidagi qisqa tavsif. */
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

    /** 150000 -> "150 000" */
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
