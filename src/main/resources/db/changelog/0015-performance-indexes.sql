--liquibase formatted sql

--changeset kitobgo:0021-performance-btree-indexes
-- Buyurtma ro'yxatlari va batch workerlar ishlatadigan filter + sort indekslari.
CREATE INDEX idx_orders_operator_status_created
    ON orders (operator_id, status, created_at DESC);
CREATE INDEX idx_orders_courier_status_created
    ON orders (courier_id, status, created_at DESC);
CREATE INDEX idx_orders_status_created
    ON orders (status, created_at DESC);
CREATE INDEX idx_orders_delivery_status_created
    ON orders (delivery_method, status, created_at ASC);
CREATE INDEX idx_orders_unassigned_created
    ON orders (created_at ASC) WHERE operator_id IS NULL;
CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);
CREATE INDEX idx_product_images_product_sort
    ON product_images (product_id, sort_order);
CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
CREATE INDEX idx_products_status_title
    ON products (status, title);
CREATE INDEX idx_users_available_operator
    ON users (role, last_seen_at, created_at, id) WHERE online = true;

--rollback DROP INDEX idx_users_available_operator;
--rollback DROP INDEX idx_products_status_title;
--rollback DROP INDEX idx_refresh_tokens_user_id;
--rollback DROP INDEX idx_product_images_product_sort;
--rollback DROP INDEX idx_order_items_order_id;
--rollback DROP INDEX idx_orders_unassigned_created;
--rollback DROP INDEX idx_orders_delivery_status_created;
--rollback DROP INDEX idx_orders_status_created;
--rollback DROP INDEX idx_orders_courier_status_created;
--rollback DROP INDEX idx_orders_operator_status_created;

--changeset kitobgo:0022-product-search-trigram-indexes
-- lower(...) LIKE '%term%' qidiruvlari uchun oddiy B-tree emas, trigram GIN kerak.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_products_title_trgm
    ON products USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_products_author_trgm
    ON products USING gin (lower(author) gin_trgm_ops);

--rollback DROP INDEX idx_products_author_trgm;
--rollback DROP INDEX idx_products_title_trgm;
