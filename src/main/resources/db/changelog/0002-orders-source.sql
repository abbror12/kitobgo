--liquibase formatted sql

-- Buyurtmaga "source" (kanal) maydonini qo'shish: WEBSITE | INSTAGRAM | TELEGRAM.
-- Mavjud buyurtmalar WEBSITE deb belgilanadi (ular sayt checkout'idan kelgan).

--changeset kitobgo:0008-orders-add-source
ALTER TABLE orders ADD COLUMN source varchar(255);
UPDATE orders SET source = 'WEBSITE' WHERE source IS NULL;
ALTER TABLE orders ALTER COLUMN source SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT orders_source_check CHECK (source IN ('WEBSITE', 'INSTAGRAM', 'TELEGRAM'));
