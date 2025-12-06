-- Add additional columns required by Master Prompt 5
ALTER TABLE idempotency_keys ADD COLUMN operation VARCHAR(255);
ALTER TABLE idempotency_keys ADD COLUMN consumed_at TIMESTAMP;
ALTER TABLE idempotency_keys ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE idempotency_keys ADD COLUMN response_status INTEGER;
ALTER TABLE idempotency_keys ADD COLUMN response_headers CLOB;
ALTER TABLE idempotency_keys ADD COLUMN status VARCHAR(50);
