package com.example.kitobgo.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "push_notification_outbox")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private LocalDateTime availableAt;

    private LocalDateTime processedAt;

    @Column(length = 1000)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (availableAt == null) {
            availableAt = now;
        }
    }

    public void markProcessed() {
        attempts++;
        processedAt = LocalDateTime.now();
        lastError = null;
    }

    public void markFailed(Exception error) {
        attempts++;
        long delaySeconds = Math.min(300, 1L << Math.min(attempts, 8));
        availableAt = LocalDateTime.now().plusSeconds(delaySeconds);
        String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        lastError = message.substring(0, Math.min(message.length(), 1000));
    }
}
