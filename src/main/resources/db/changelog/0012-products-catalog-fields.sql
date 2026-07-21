--liquibase formatted sql

--changeset kitobgo:0018-products-catalog-fields
ALTER TABLE products
    ADD COLUMN isbn varchar(13),
    ADD COLUMN publisher varchar(255),
    ADD COLUMN language varchar(3),
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE products
    ADD CONSTRAINT products_isbn_unique UNIQUE (isbn),
    ADD CONSTRAINT products_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));

--rollback ALTER TABLE products DROP CONSTRAINT products_status_check;
--rollback ALTER TABLE products DROP CONSTRAINT products_isbn_unique;
--rollback ALTER TABLE products DROP COLUMN updated_at;
--rollback ALTER TABLE products DROP COLUMN created_at;
--rollback ALTER TABLE products DROP COLUMN status;
--rollback ALTER TABLE products DROP COLUMN language;
--rollback ALTER TABLE products DROP COLUMN publisher;
--rollback ALTER TABLE products DROP COLUMN isbn;
