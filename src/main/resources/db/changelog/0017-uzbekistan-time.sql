--liquibase formatted sql

--changeset kitobgo:0024-convert-existing-utc-timestamps-to-uzbekistan-time
-- Avvalgi app konteyneri UTC'da ishlagan va timestamp without time zone ustunlariga
-- UTC devor vaqtini yozgan. Endi loyiha Asia/Tashkent (+05:00) ishlatadi; mavjud
-- yozuvlar yangi yozuvlar bilan bir xil semantikada bo'lishi uchun bir marta +5 soat.
UPDATE users SET
    created_at = created_at + INTERVAL '5 hours',
    last_seen_at = last_seen_at + INTERVAL '5 hours';

UPDATE products SET
    created_at = created_at + INTERVAL '5 hours',
    updated_at = updated_at + INTERVAL '5 hours';

UPDATE orders SET
    created_at = created_at + INTERVAL '5 hours';

UPDATE product_images SET
    created_at = created_at + INTERVAL '5 hours';

UPDATE device_tokens SET
    created_at = created_at + INTERVAL '5 hours',
    updated_at = updated_at + INTERVAL '5 hours';

UPDATE refresh_tokens SET
    created_at = created_at + INTERVAL '5 hours',
    expires_at = expires_at + INTERVAL '5 hours';

UPDATE emu_shipments SET
    created_at = created_at + INTERVAL '5 hours',
    exported_at = exported_at + INTERVAL '5 hours';

UPDATE order_status_history SET
    changed_at = changed_at + INTERVAL '5 hours';

UPDATE push_notification_outbox SET
    created_at = created_at + INTERVAL '5 hours',
    available_at = available_at + INTERVAL '5 hours',
    processed_at = processed_at + INTERVAL '5 hours';

--rollback UPDATE push_notification_outbox SET created_at = created_at - INTERVAL '5 hours', available_at = available_at - INTERVAL '5 hours', processed_at = processed_at - INTERVAL '5 hours';
--rollback UPDATE order_status_history SET changed_at = changed_at - INTERVAL '5 hours';
--rollback UPDATE emu_shipments SET created_at = created_at - INTERVAL '5 hours', exported_at = exported_at - INTERVAL '5 hours';
--rollback UPDATE refresh_tokens SET created_at = created_at - INTERVAL '5 hours', expires_at = expires_at - INTERVAL '5 hours';
--rollback UPDATE device_tokens SET created_at = created_at - INTERVAL '5 hours', updated_at = updated_at - INTERVAL '5 hours';
--rollback UPDATE product_images SET created_at = created_at - INTERVAL '5 hours';
--rollback UPDATE orders SET created_at = created_at - INTERVAL '5 hours';
--rollback UPDATE products SET created_at = created_at - INTERVAL '5 hours', updated_at = updated_at - INTERVAL '5 hours';
--rollback UPDATE users SET created_at = created_at - INTERVAL '5 hours', last_seen_at = last_seen_at - INTERVAL '5 hours';
