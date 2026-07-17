--liquibase formatted sql

-- users_role_check constraint'iga yangi SMM_MANAGER rolini qo'shish
-- (Instagram/Telegram lead'lari bilan ishlab, qo'lda buyurtma yaratadigan xodim).

--changeset kitobgo:0009-users-role-add-smm-manager
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'COURIER', 'OPERATOR', 'SMM_MANAGER'));
