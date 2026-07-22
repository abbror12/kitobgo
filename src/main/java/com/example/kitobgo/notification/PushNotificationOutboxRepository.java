package com.example.kitobgo.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PushNotificationOutboxRepository extends JpaRepository<PushNotificationOutbox, UUID> {

    /** Bir nechta app instance bir xil xabarni olmasligi uchun row lock + SKIP LOCKED. */
    @Query(value = """
            select *
            from push_notification_outbox
            where processed_at is null
              and attempts < :maxAttempts
              and available_at <= current_timestamp
            order by created_at asc
            for update skip locked
            limit :batchSize
            """, nativeQuery = true)
    List<PushNotificationOutbox> lockNextBatch(
            @Param("batchSize") int batchSize,
            @Param("maxAttempts") int maxAttempts);
}
