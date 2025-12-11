-- Flyway V1 migration: create orders and audit_logs tables for H2
-- Flyway V1 migration: create orders, transactions, idempotency_keys, webhook_events and audit_logs tables for H2

CREATE TABLE IF NOT EXISTS orders (
  id VARCHAR(64) PRIMARY KEY,
  external_order_id VARCHAR(128),
  customer_id VARCHAR(64),
  amount_cents BIGINT,
  currency VARCHAR(8),
  status VARCHAR(32),
  version BIGINT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
  id VARCHAR(64) PRIMARY KEY,
  order_id VARCHAR(64) NOT NULL,
  amount_cents BIGINT,
  currency VARCHAR(8),
  type VARCHAR(32),
  status VARCHAR(32),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
  id VARCHAR(128) PRIMARY KEY,
  request_hash VARCHAR(256),
  response_body TEXT,
  created_at TIMESTAMP,
  version BIGINT
);

CREATE TABLE IF NOT EXISTS webhook_events (
  id VARCHAR(64) PRIMARY KEY,
  source VARCHAR(128),
  payload TEXT,
  status VARCHAR(32),
  received_at TIMESTAMP,
  processed_at TIMESTAMP,
  version BIGINT
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64),
  action VARCHAR(128),
  resource_type VARCHAR(64),
  resource_id VARCHAR(64),
  metadata TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT
);
