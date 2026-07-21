--liquibase formatted sql

--changeset kitobgo:0020-orders-idempotency
ALTER TABLE orders
    ADD COLUMN client_request_id uuid,
    ADD COLUMN client_request_hash varchar(64);

ALTER TABLE orders
    ADD CONSTRAINT orders_client_request_id_unique UNIQUE (client_request_id),
    ADD CONSTRAINT orders_client_request_pair_check CHECK (
        (client_request_id IS NULL AND client_request_hash IS NULL)
        OR (client_request_id IS NOT NULL AND client_request_hash IS NOT NULL)
    );

--rollback ALTER TABLE orders DROP CONSTRAINT orders_client_request_pair_check;
--rollback ALTER TABLE orders DROP CONSTRAINT orders_client_request_id_unique;
--rollback ALTER TABLE orders DROP COLUMN client_request_hash;
--rollback ALTER TABLE orders DROP COLUMN client_request_id;
