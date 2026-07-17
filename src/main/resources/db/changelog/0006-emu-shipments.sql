--liquibase formatted sql

-- EMU pasilkalari alohida jadvalga ajratildi. Ilgari trek-raqam orders.tracking_number
-- da edi va har bir COURIER buyurtmasida bo'sh turardi; API integratsiyasi bilan yana
-- bir nechta EMU maydoni qo'shilishi kerak edi. Endi EMU holati faqat EMU ga
-- topshirilgan buyurtmalarda — emu_shipments qatorida yashaydi.

--changeset kitobgo:0012-emu-shipments-create
CREATE TABLE emu_shipments (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    parcel_name varchar(255) NOT NULL,
    tracking_number varchar(255),
    exported_at timestamp(6),
    created_at timestamp(6),
    CONSTRAINT emu_shipments_pkey PRIMARY KEY (id),
    CONSTRAINT emu_shipments_order_unique UNIQUE (order_id),
    CONSTRAINT emu_shipments_order_fkey FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- Trek-raqami bor mavjud buyurtmalar EMU ga allaqachon topshirilgan — ularga pasilka
-- yozuvi yaratamiz. parcel_name NOT NULL bo'lgani uchun nom shu yerda buyurtma
-- qatorlaridan yasaladi: EmuParcelName bilan bir xil qoida ("<birinchi kitob>" yoki
-- "<birinchi kitob> va yana N ta kitob"). Uzunlikni kesish qo'llanmaydi — eski
-- yozuvlar oz va varchar(255) ga sig'adi.
--changeset kitobgo:0012-emu-shipments-backfill
INSERT INTO emu_shipments (id, order_id, parcel_name, tracking_number, exported_at, created_at)
SELECT
    gen_random_uuid(),
    o.id,
    CASE
        WHEN qty.total > 1 THEN title.name || ' va yana ' || (qty.total - 1) || ' ta kitob'
        ELSE title.name
    END,
    o.tracking_number,
    o.created_at,
    o.created_at
FROM orders o
CROSS JOIN LATERAL (
    SELECT COALESCE(SUM(oi.quantity), 0) AS total
    FROM order_items oi
    WHERE oi.order_id = o.id
) qty
CROSS JOIN LATERAL (
    SELECT COALESCE(NULLIF(TRIM(p.title), ''), 'Kitob') AS name
    FROM order_items oi
    LEFT JOIN products p ON p.id = oi.product_id
    WHERE oi.order_id = o.id
    LIMIT 1
) title
WHERE o.tracking_number IS NOT NULL;

-- Qatorlari umuman yo'q buyurtma (LATERAL hech narsa qaytarmaydi) yuqoridagi INSERT'ga
-- tushmaydi — bunday buyurtma bo'lsa trek-raqami bilan qoladi, shuning uchun ustunni
-- o'chirishdan oldin zaxira nom bilan qo'shamiz.
--changeset kitobgo:0012-emu-shipments-backfill-itemless
INSERT INTO emu_shipments (id, order_id, parcel_name, tracking_number, exported_at, created_at)
SELECT gen_random_uuid(), o.id, 'Kitob', o.tracking_number, o.created_at, o.created_at
FROM orders o
WHERE o.tracking_number IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM emu_shipments s WHERE s.order_id = o.id);

--changeset kitobgo:0012-orders-drop-tracking-number
ALTER TABLE orders DROP COLUMN tracking_number;
