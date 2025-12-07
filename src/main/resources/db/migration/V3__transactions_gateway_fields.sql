-- Flyway V3: add gateway-related columns to transactions
ALTER TABLE transactions ADD COLUMN gateway VARCHAR(128);
ALTER TABLE transactions ADD COLUMN gateway_transaction_id VARCHAR(128);
ALTER TABLE transactions ADD COLUMN gateway_response CLOB;
ALTER TABLE transactions ADD COLUMN gateway_message VARCHAR(512);
