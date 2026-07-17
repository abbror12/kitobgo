package com.example.kitobgo.auth;

import com.example.kitobgo.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    /** Foydalanuvchining muddati o'tgan tokenlarini tozalash (login paytida chaqiriladi). */
    void deleteByUserAndExpiresAtBefore(User user, LocalDateTime time);
}
