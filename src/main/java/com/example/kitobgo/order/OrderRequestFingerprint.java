package com.example.kitobgo.order;

import com.example.kitobgo.order.dto.OrderItemRequest;
import com.example.kitobgo.order.dto.OrderRequestDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;

/** Bir Idempotency-Key boshqa checkout payload'i bilan ishlatilmasligi uchun fingerprint. */
final class OrderRequestFingerprint {

    private OrderRequestFingerprint() {
    }

    static String sha256(OrderRequestDto dto) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, dto.customerName());
        append(canonical, dto.customerPhone());
        append(canonical, dto.region() != null ? dto.region().name() : null);

        if (dto.items() != null) {
            dto.items().stream()
                    .sorted(Comparator
                            .comparing(OrderItemRequest::productId,
                                    Comparator.nullsFirst(Comparator.naturalOrder()))
                            .thenComparing(item -> item.quantity() != null ? item.quantity() : 1))
                    .forEach(item -> {
                        append(canonical, item.productId() != null ? item.productId().toString() : null);
                        append(canonical, Integer.toString(item.quantity() != null ? item.quantity() : 1));
                    });
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 JVM tomonidan qo'llab-quvvatlanmadi", e);
        }
    }

    private static void append(StringBuilder target, String value) {
        String safe = value != null ? value : "";
        target.append(safe.length()).append(':').append(safe).append('|');
    }
}
