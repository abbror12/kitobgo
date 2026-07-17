--liquibase formatted sql

-- Baseline: Hibernate (ddl-auto: update) yaratgan mavjud sxemaning aynan nusxasi.
-- Har changeset'da "jadval allaqachon bor bo'lsa — qo'llangan deb belgila" (MARK_RAN)
-- sharti bor: mavjud bazada hech narsa o'zgarmaydi, yangi bo'sh bazada sxema yaratiladi.

--changeset kitobgo:0001-users
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='users'
CREATE TABLE users (
    id uuid NOT NULL,
    created_at timestamp(6),
    last_seen_at timestamp(6),
    name varchar(255),
    online boolean,
    password varchar(255),
    phone varchar(255),
    role varchar(255),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_phone_unique UNIQUE (phone),
    CONSTRAINT users_role_check CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'COURIER', 'OPERATOR'))
);

--changeset kitobgo:0002-products
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='products'
CREATE TABLE products (
    id uuid NOT NULL,
    author varchar(255),
    description varchar(255),
    discount_price integer,
    page_count integer,
    price integer,
    published_year integer,
    rating real,
    stock_quantity integer,
    title varchar(255),
    version bigint NOT NULL,
    CONSTRAINT products_pkey PRIMARY KEY (id)
);

--changeset kitobgo:0003-orders
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='orders'
CREATE TABLE orders (
    id uuid NOT NULL,
    address varchar(255),
    confirmed_at timestamp(6),
    created_at timestamp(6),
    customer_name varchar(255),
    customer_phone varchar(255),
    delivered_at timestamp(6),
    in_delivery_at timestamp(6),
    cancelled_at timestamp(6),
    returned_at timestamp(6),
    reprocessing_at timestamp(6),
    status varchar(255),
    courier_id uuid,
    operator_id uuid,
    CONSTRAINT orders_pkey PRIMARY KEY (id),
    CONSTRAINT orders_status_check CHECK (status IN ('NEW', 'CONFIRMED', 'IN_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED', 'REPROCESSING')),
    CONSTRAINT orders_operator_fk FOREIGN KEY (operator_id) REFERENCES users (id),
    CONSTRAINT orders_courier_fk FOREIGN KEY (courier_id) REFERENCES users (id)
);

--changeset kitobgo:0004-order-items
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='order_items'
CREATE TABLE order_items (
    id uuid NOT NULL,
    price_at_purchase integer,
    quantity integer,
    order_id uuid,
    product_id uuid,
    CONSTRAINT order_items_pkey PRIMARY KEY (id),
    CONSTRAINT order_items_order_fk FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT order_items_product_fk FOREIGN KEY (product_id) REFERENCES products (id)
);

--changeset kitobgo:0005-product-images
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='product_images'
CREATE TABLE product_images (
    id uuid NOT NULL,
    created_at timestamp(6),
    sort_order integer,
    url varchar(255),
    product_id uuid,
    CONSTRAINT product_images_pkey PRIMARY KEY (id),
    CONSTRAINT product_images_product_fk FOREIGN KEY (product_id) REFERENCES products (id)
);

--changeset kitobgo:0006-device-tokens
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='device_tokens'
CREATE TABLE device_tokens (
    id uuid NOT NULL,
    created_at timestamp(6),
    platform varchar(255),
    token varchar(512) NOT NULL,
    updated_at timestamp(6),
    user_id uuid NOT NULL,
    CONSTRAINT device_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT device_tokens_token_unique UNIQUE (token),
    CONSTRAINT device_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

--changeset kitobgo:0007-refresh-tokens
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='refresh_tokens'
CREATE TABLE refresh_tokens (
    id uuid NOT NULL,
    created_at timestamp(6),
    expires_at timestamp(6) NOT NULL,
    token varchar(128) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_token_unique UNIQUE (token),
    CONSTRAINT refresh_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users (id)
);
