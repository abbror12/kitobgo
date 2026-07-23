package com.example.kitobgo.auth;

import com.example.kitobgo.common.AppTime;
import com.example.kitobgo.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uzoq muddatli refresh token — access token (JWT) muddati tugaganda qaytadan
 * login qilmasdan yangi token olish uchun. Bazada saqlanadi, shuning uchun
 * istalgan payti bekor qilish (logout) mumkin. Har ishlatilganda yangisiga
 * almashtiriladi (rotation) — o'g'irlangan token qayta ishlamaydi.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Tasodifiy opaque qiymat — mijozga beriladi, JWT emas. */
    @Column(unique = true, nullable = false, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = AppTime.now();
    }
}
