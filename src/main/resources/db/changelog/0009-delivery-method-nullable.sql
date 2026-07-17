--liquibase formatted sql

-- Marshrut (delivery_method) endi buyurtma yaratilganda emas, TASDIQLANGANDA qo'yiladi:
-- mijoz saytda viloyat tanlamaydi (u noto'g'ri tanlashi mumkin), operator gaplashib
-- viloyatni aniqlaydi, tasdiqlashda esa marshrut Region.autoRoute() orqali chiqadi.
-- Shu sababli ustun endi bo'sh bo'lishi mumkin:
--   * tasdiqlanmagan buyurtma        — marshrut hali yo'q;
--   * Toshkent viloyati buyurtmasi   — EMU ham, kuryer ham mumkin, admin tanlaydi.
-- Mavjud qatorlar o'z qiymatini saqlaydi.

--changeset kitobgo:0015-orders-delivery-method-nullable
ALTER TABLE orders ALTER COLUMN delivery_method DROP NOT NULL;
