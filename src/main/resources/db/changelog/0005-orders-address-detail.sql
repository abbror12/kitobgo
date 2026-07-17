--liquibase formatted sql

-- Tuzilgan manzil maydonlari: district (tuman) va landmark (mo'ljal).
-- Mavjud "address" ustuni endi ko'cha/uy qatorи sifatida ishlatiladi.

--changeset kitobgo:0011-orders-add-address-detail
ALTER TABLE orders ADD COLUMN district varchar(255);
ALTER TABLE orders ADD COLUMN landmark varchar(255);
