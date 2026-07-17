package com.example.kitobgo.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    @Query("select dt.token from DeviceToken dt where dt.user.id = :userId")
    List<String> findTokensByUserId(UUID userId);

    /** Yaroqsiz (UNREGISTERED) token'larni tozalash — tranzaksiyadan tashqarida ham chaqirilishi mumkin. */
    @Transactional
    void deleteByTokenIn(Collection<String> tokens);
}
