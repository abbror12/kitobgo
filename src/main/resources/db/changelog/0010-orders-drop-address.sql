--liquibase formatted sql

-- "address" (ko'cha/uy) ustuni olib tashlanadi. Amalda mijozlar ko'cha nomini emas,
-- mo'ljalni aytadi — manzil endi region + district + landmark dan yig'iladi
-- (qarang Order dagi "Yetkazish manzili" izohi).
--
-- Ustundagi ma'lumot yo'qoladi, shuning uchun o'chirishdan oldin u landmark ga
-- ko'chiriladi: landmark bo'sh bo'lsa address o'sha yerga o'tadi, ikkalasi ham
-- to'la bo'lsa birlashtiriladi.

--changeset kitobgo:0016-orders-move-address-to-landmark
UPDATE orders
SET landmark = CASE
    WHEN landmark IS NULL OR btrim(landmark) = '' THEN address
    ELSE address || ', ' || landmark
END
WHERE address IS NOT NULL AND btrim(address) <> '';

--changeset kitobgo:0016-orders-drop-address
ALTER TABLE orders DROP COLUMN IF EXISTS address;
