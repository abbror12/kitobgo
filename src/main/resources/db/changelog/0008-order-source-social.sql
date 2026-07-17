--liquibase formatted sql

-- INSTAGRAM va TELEGRAM bitta SOCIAL_NETWORK'ga birlashtirildi. Ular kodda hech qayerda
-- farqlanmasdi — butun mantiq "WEBSITE mi yoki yo'qmi" degan shartga qurilgan edi
-- (avto-taqsimot faqat WEBSITE uchun). Qaysi ijtimoiy tarmoqdan kelgani saqlanmaydi.

-- IF EXISTS — constraint biror sababdan yo'q bo'lsa migratsiya qulamasin
-- (aks holda butun ilova ishga tushmaydi).
--changeset kitobgo:0014-orders-source-social-network
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_source_check;
UPDATE orders SET source = 'SOCIAL_NETWORK' WHERE source IN ('INSTAGRAM', 'TELEGRAM');
ALTER TABLE orders ADD CONSTRAINT orders_source_check CHECK (source IN ('WEBSITE', 'SOCIAL_NETWORK'));
