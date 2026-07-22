package com.example.kitobgo.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationOutboxWorker {

    private final PushNotificationOutboxProcessor processor;

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-interval-ms:1000}")
    public void poll() {
        try {
            processor.processNextBatch();
        } catch (Exception error) {
            // Worker keyingi intervalda yana urinadi; scheduler thread o'lib qolmasligi kerak.
            log.error("Push outbox batchini qayta ishlashda xato", error);
        }
    }
}
