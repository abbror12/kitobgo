--liquibase formatted sql

--changeset kitobgo:0023-push-notification-outbox
CREATE TABLE push_notification_outbox (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    order_id uuid NOT NULL,
    type varchar(40) NOT NULL,
    title varchar(255) NOT NULL,
    body varchar(1000) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    available_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at timestamp(6),
    last_error varchar(1000),
    created_at timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT push_notification_outbox_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_push_outbox_pending
    ON push_notification_outbox (available_at, created_at)
    WHERE processed_at IS NULL;

--rollback DROP TABLE push_notification_outbox;
