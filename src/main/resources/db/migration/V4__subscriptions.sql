-- Flyway migration: create subscriptions table for local testing
CREATE TABLE IF NOT EXISTS subscriptions (
  id VARCHAR(255) PRIMARY KEY,
  customer_id VARCHAR(255),
  plan_id VARCHAR(255),
  amount_cents BIGINT,
  currency VARCHAR(10),
  status VARCHAR(50),
  interval_days INTEGER,
  next_billing_at TIMESTAMP,
  last_renewed_at TIMESTAMP,
  created_at TIMESTAMP,
  version BIGINT
);
