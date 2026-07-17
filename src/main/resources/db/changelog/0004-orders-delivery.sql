--liquibase formatted sql

-- Yetkazish maydonlari: region (viloyat), delivery_method (COURIER|EMU), tracking_number.
-- Mavjud buyurtmalar COURIER deb belgilanadi (EMU'gacha yaratilgan); region ularда null.

--changeset kitobgo:0010-orders-add-delivery
ALTER TABLE orders ADD COLUMN region varchar(255);
ALTER TABLE orders ADD COLUMN delivery_method varchar(255);
ALTER TABLE orders ADD COLUMN tracking_number varchar(255);
UPDATE orders SET delivery_method = 'COURIER' WHERE delivery_method IS NULL;
ALTER TABLE orders ALTER COLUMN delivery_method SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT orders_delivery_method_check CHECK (delivery_method IN ('COURIER', 'EMU'));
ALTER TABLE orders ADD CONSTRAINT orders_region_check CHECK (region IN (
    'TASHKENT_CITY', 'TASHKENT_REGION', 'ANDIJAN', 'FERGANA', 'NAMANGAN', 'SIRDARYO',
    'JIZZAKH', 'SAMARKAND', 'KASHKADARYO', 'SURKHANDARYO', 'BUKHARA', 'NAVOI',
    'KHOREZM', 'KARAKALPAKSTAN'));
