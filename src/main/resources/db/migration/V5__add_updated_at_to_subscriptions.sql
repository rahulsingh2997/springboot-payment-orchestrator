-- Flyway migration: add updated_at to subscriptions
ALTER TABLE subscriptions ADD COLUMN updated_at TIMESTAMP;