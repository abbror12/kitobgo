--liquibase formatted sql

-- Status vaqtlari alohida jadvalga ajratildi. Ilgari orders'da 6 ta ustun bor edi
-- (confirmed_at, in_delivery_at, delivered_at, cancelled_at, returned_at,
-- reprocessing_at) va har o'tish o'z ustuniga yozilardi. Statuslar orasida o'tish
-- grafi yo'q, ya'ni takroriy o'tish mumkin (kuryer qaytardi -> operator qayta ishladi
-- -> yana qaytardi) va bunda oldingi vaqt o'chib ketardi. Kim o'zgartirgani esa
-- umuman saqlanmasdi. Endi har o'tish alohida qator: hech narsa yo'qolmaydi.
--
-- orders.status ustuni QOLADI — joriy holat bo'yicha query'lar (findByStatus...) uchun.

--changeset kitobgo:0013-order-status-history-create
CREATE TABLE order_status_history (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    status varchar(255) NOT NULL,
    changed_by uuid,
    changed_at timestamp(6) NOT NULL,
    CONSTRAINT order_status_history_pkey PRIMARY KEY (id),
    CONSTRAINT order_status_history_order_fkey FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT order_status_history_user_fkey FOREIGN KEY (changed_by) REFERENCES users (id),
    CONSTRAINT order_status_history_status_check CHECK (status IN (
        'NEW', 'CONFIRMED', 'IN_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REPROCESSING'))
);
CREATE INDEX order_status_history_order_idx ON order_status_history (order_id);

-- Mavjud ustunlardan tarix yasaymiz. changed_by hamma yerda NULL — kim o'zgartirgani
-- hech qachon saqlanmagan, uni yo'qdan tiklab bo'lmaydi. Yo'qolgan takroriy o'tishlar
-- ham qaytmaydi: har ustunda faqat oxirgi qiymat qolgan. Tarix bugundan to'liq bo'ladi.
--changeset kitobgo:0013-order-status-history-backfill
INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'NEW', NULL, o.created_at
FROM orders o WHERE o.created_at IS NOT NULL;

INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'CONFIRMED', NULL, o.confirmed_at
FROM orders o WHERE o.confirmed_at IS NOT NULL;

INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'IN_DELIVERY', NULL, o.in_delivery_at
FROM orders o WHERE o.in_delivery_at IS NOT NULL;

INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'DELIVERED', NULL, o.delivered_at
FROM orders o WHERE o.delivered_at IS NOT NULL;

INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'CANCELLED', NULL, o.cancelled_at
FROM orders o WHERE o.cancelled_at IS NOT NULL;

INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'RETURNED', NULL, o.returned_at
FROM orders o WHERE o.returned_at IS NOT NULL;

INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, 'REPROCESSING', NULL, o.reprocessing_at
FROM orders o WHERE o.reprocessing_at IS NOT NULL;

-- Joriy statusi tarixga tushmagan buyurtmalar (vaqt ustuni bo'sh qolgan eski yozuvlar)
-- bo'lsa, ular tarixsiz qolib ketmasin: joriy statusni created_at vaqti bilan qo'shamiz.
-- Vaqt taxminiy, lekin buyurtmaning hozirgi holati tarixda ko'rinishi muhimroq.
--changeset kitobgo:0013-order-status-history-current-status-fallback
INSERT INTO order_status_history (id, order_id, status, changed_by, changed_at)
SELECT gen_random_uuid(), o.id, o.status, NULL, COALESCE(o.created_at, now())
FROM orders o
WHERE o.status IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM order_status_history h
      WHERE h.order_id = o.id AND h.status = o.status
  );

--changeset kitobgo:0013-orders-drop-status-timestamps
ALTER TABLE orders DROP COLUMN confirmed_at;
ALTER TABLE orders DROP COLUMN in_delivery_at;
ALTER TABLE orders DROP COLUMN delivered_at;
ALTER TABLE orders DROP COLUMN cancelled_at;
ALTER TABLE orders DROP COLUMN returned_at;
ALTER TABLE orders DROP COLUMN reprocessing_at;
