package com.example.kitobgo.order;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Bir xil Idempotency-Key bilan parallel kelgan requestlarni PostgreSQL transaction-level
 * advisory lock orqali ketma-ketlashtiradi. Lock transaction tugaganda avtomatik bo'shaydi.
 */
@Component
@RequiredArgsConstructor
public class OrderIdempotencyLock {

    private final EntityManager entityManager;

    public void acquire(UUID clientRequestId) {
        long lockKey = clientRequestId.getMostSignificantBits()
                ^ Long.rotateLeft(clientRequestId.getLeastSignificantBits(), 32);
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
