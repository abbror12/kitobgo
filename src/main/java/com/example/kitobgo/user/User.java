package com.example.kitobgo.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    @Column(unique = true)
    private String phone;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Operator/kuryer online holati — mobil ilova toggle qiladi (ishni boshlash/tugatish).
     * {@code null}/{@code false} = offline. Buyurtma faqat online (va heartbeat'i tirik)
     * operatorlarga taqsimlanadi.
     */
    @Builder.Default
    private Boolean online = false;

    /**
     * Oxirgi faollik (heartbeat) vaqti. {@code online=true} bo'lsa ham, agar lastSeenAt
     * timeout'dan eski bo'lsa (ilova yopilib qolgan) operator "mavjud emas" hisoblanadi.
     */
    private LocalDateTime lastSeenAt;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
