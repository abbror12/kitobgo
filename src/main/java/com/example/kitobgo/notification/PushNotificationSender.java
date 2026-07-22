package com.example.kitobgo.notification;

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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** FCM bilan tarmoq aloqasi faqat outbox worker tomonidan shu komponentda bajariladi. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationSender {

    private static final String CHANNEL_ID = "orders";

    private final DeviceTokenRepository deviceTokenRepository;

    public void send(PushNotificationOutbox outbox) throws Exception {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("FCM sozlanmagan — outbox xabari tashlab ketildi (id={})", outbox.getId());
            return;
        }

        List<String> tokens = deviceTokenRepository.findTokensByUserId(outbox.getUserId());
        if (tokens.isEmpty()) {
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(outbox.getTitle())
                        .setBody(outbox.getBody())
                        .build())
                .putData("type", outbox.getType())
                .putData("orderId", outbox.getOrderId().toString())
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
                outbox.getType(), outbox.getOrderId(), response.getSuccessCount(), tokens.size());
    }

    private void cleanupInvalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalid = new ArrayList<>();
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse result = responses.get(i);
            if (!result.isSuccessful() && result.getException() != null) {
                MessagingErrorCode code = result.getException().getMessagingErrorCode();
                log.warn("FCM token rad etildi: kod={}, xabar={}, token=...{}",
                        code, result.getException().getMessage(), tail(tokens.get(i)));
                if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    invalid.add(tokens.get(i));
                }
            }
        }
        if (!invalid.isEmpty()) {
            deviceTokenRepository.deleteByTokenIn(invalid);
        }
    }

    private String tail(String token) {
        return token.length() <= 8 ? token : token.substring(token.length() - 8);
    }
}
