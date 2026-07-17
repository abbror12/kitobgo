package com.example.kitobgo.notification;

import com.example.kitobgo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Foydalanuvchi qurilmasining FCM registration token'i. Bir foydalanuvchida
 * bir nechta qurilma bo'lishi mumkin; bitta token esa faqat bitta foydalanuvchiga
 * tegishli (unique) — boshqa hisob kirsa token shu hisobga qayta biriktiriladi.
 */
@Entity
@Table(name = "device_tokens", indexes = @Index(name = "idx_device_tokens_user_id", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    private String platform;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
