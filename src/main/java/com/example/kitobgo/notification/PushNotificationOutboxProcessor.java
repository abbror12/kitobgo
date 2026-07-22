package com.example.kitobgo.notification;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationOutboxProcessor {

    private final PushNotificationOutboxRepository outboxRepository;
    private final PushNotificationSender sender;
    private final MeterRegistry meterRegistry;

    @Value("${app.notification.outbox.batch-size:50}")
    private int batchSize;

    @Value("${app.notification.outbox.max-attempts:8}")
    private int maxAttempts;

    /**
     * Lock olingan xabarlarni bitta kichik tranzaksiyada yuboradi. SKIP LOCKED tufayli
     * boshqa instance shu paytda boshqa batchni xavfsiz qayta ishlashi mumkin.
     */
    @Transactional
    public int processNextBatch() {
        List<PushNotificationOutbox> batch = outboxRepository.lockNextBatch(batchSize, maxAttempts);
        for (PushNotificationOutbox outbox : batch) {
            try {
                sender.send(outbox);
                outbox.markProcessed();
                meterRegistry.counter("kitobgo.push.outbox.sent", "type", outbox.getType()).increment();
            } catch (Exception error) {
                outbox.markFailed(error);
                meterRegistry.counter("kitobgo.push.outbox.failed", "type", outbox.getType()).increment();
                log.warn("Push outbox qayta urinadi: id={}, attempt={}/{}",
                        outbox.getId(), outbox.getAttempts(), maxAttempts, error);
            }
        }
        return batch.size();
    }
}
