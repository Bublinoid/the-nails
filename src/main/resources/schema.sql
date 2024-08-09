-- Создание схемы the_nails
CREATE SCHEMA IF NOT EXISTS the_nails;

-- Создание таблицы email в схеме the_nails
CREATE TABLE IF NOT EXISTS the_nails.email (
hash UUID PRIMARY KEY,
chat_id BIGINT NOT NULL,
email VARCHAR(255) NOT NULL,
confirmation_code VARCHAR(4),
confirm BOOLEAN,
insert_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
