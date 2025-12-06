# Project Blueprint: springboot-payment-orchestrator

This single-file blueprint documents the planned deliverables across three phases for the Spring Boot payment orchestrator integrating with Authorize.Net Sandbox. It covers backend modules, database schema changes, API endpoints, data flows, error handling, security, testing strategy, compliance notes, observability SLAs, developer experience (DX) improvements, and expected repository artifacts per phase.

---

## Overview

Goal: Build a robust backend application that integrates with Authorize.Net Sandbox and supports purchase (auth+capture), authorize→capture, cancel/void, refunds (full/partial), recurring billing/subscriptions, idempotency, webhook handling, JWT auth, DB persistence, distributed tracing, and observability. Deliverables must include specific documentation files and Docker Compose for validation.

API versioning: All consumer-facing endpoints will be versioned under `/api/v1/` to allow safe iterative improvements (e.g. `/api/v1/payments/purchase`). Internal worker endpoints (webhooks, admin) follow the same versioning pattern where applicable.

Core choices (recommended): Java 17 (or Java 8 if constrained by environment), Spring Boot 3.x (or 2.7.x for Java 8), PostgreSQL, RabbitMQ (dev queue), Authorize.Net official Java SDK, OpenTelemetry + Micrometer, JaCoCo for coverage, GitHub Actions for CI.

---

## Phase 1 — Foundation

### Objectives
- Create a production-capable Spring Boot skeleton and local dev infra.
- Implement DB migrations, JPA entities for orders & transactions, idempotency store, JWT auth scaffold, correlation ID propagation, structured logging, and a dev `docker-compose` bringing up Postgres, RabbitMQ, and Jaeger.

### Developer experience (DX) enhancements
- Add Spring DevTools to speed up development reloads.
- Support `.env` files for sandbox credentials and local overrides (via `spring.config.import=optional:dotenv:` or `dotenv-java` helper), documented in `README.md`.
- Provide Makefile helpers for common developer tasks: `make up` (start dev stack), `make test` (run unit tests), `make coverage` (run tests + generate coverage report), and `make ci` (CI-like checks).

### Backend modules (to implement)
- `api` — Controllers, DTOs and validation for basic endpoints (health, sample order) under `/api/v1/` prefix.
- `config` — DB, security placeholders, Authorize.Net client config placeholders.
- `common` — Correlation-ID filter, MDC utilities, error DTOs, exception handlers.
- `persistence` — JPA entities, repositories, Flyway migrations.
- `security` — Spring Security config and JWT filter (dev-friendly HS256 placeholder; recommend RS256 for prod).
- `idempotency` — Idempotency key service and interceptor, idempotency table abstraction.
- `audit` — Audit log writer and minimal persistence.
- `dev` — Docker Compose helper configs and sample properties for dev profile.

### Database tables / entities
- `orders` — id (UUID), external_order_id, customer_id, amount_cents, currency, status, created_at, updated_at, version.
  - Note: `status` will be tied to the Order State Machine described in Phase 2 (PENDING, AUTHORIZED, CAPTURED, VOIDED, REFUNDED, FAILED, CANCELLED).
- `transactions` — id (UUID), order_id (FK), type, authorize_net_txn_id, amount_cents, status, response_payload (JSON), gateway_response (redacted JSON), created_at.
- `idempotency_keys` — key, operation, request_hash, created_by, created_at, expires_at, consumed_at, response_snapshot (JSON), status.
- `webhook_events` — id, event_id, payload (JSON), received_at, processed_at, attempts, status.
- `audit_logs` — id, user_id, action, resource_type, resource_id, metadata (JSON), timestamp.
- Flyway migration: `V1__init.sql` creating above tables.

### API endpoints (initial)
- `GET /actuator/health` (Spring Actuator)
- `GET /api/v1/health` — application-level health check (DB connectivity)
- `POST /api/v1/orders` — create new order (persist only)
- `GET /api/v1/orders/{orderId}` — fetch order details

### Correlation ID and tracing design
- The system will accept and propagate `X-Correlation-ID`. Define the canonical format as: `app-{uuid}-{timestamp}` (e.g., `app-3fa85f64-5717-4562-b3fc-2c963f66afa6-1700000000`). Services should validate incoming values and create one if missing. The correlation id is stored in MDC and propagated to downstream calls and queued messages.

### Data flow & sequence (text)
- Create Order:
  1. Client -> POST `/api/v1/orders` (JWT + `Idempotency-Key`)
  2. Controller -> Idempotency interceptor -> `X-Correlation-ID` extraction/creation
  3. Service -> OrderRepository.save() -> DB (order status = PENDING)
  4. AuditLog saved -> respond 201 with `orderId` and `X-Correlation-ID` header

### Error-handling strategy
- Centralized `@ControllerAdvice` mapping exceptions to HTTP codes.
- Standard error response: `timestamp`, `status`, `error`, `message`, `path`, `correlationId`.
- `ValidationException` -> 400, `IdempotencyConflictException` -> 409 (return previous snapshot), `ResourceNotFoundException` -> 404, unexpected -> 500 (sanitized message).

### Security layers
- JWT authentication scaffold (dev HS256; recommend RS256 in production with key rotation).
- Role-based placeholders (`ROLE_USER`, `ROLE_ADMIN`).
- Correlation-ID extraction/creation (`X-Correlation-ID`) and MDC wiring.
- Input validation via Bean Validation.

### Testing strategy (Phase 1)
- Unit tests for controllers, services, and idempotency middleware.
- Repository tests using H2 or Testcontainers Postgres for parity.
- JaCoCo configured; baseline coverage checks.

### Compliance considerations
- Do not store PAN/CVV. Use tokenization only.
- Logging policy: redact sensitive fields; store `last4` and token IDs only.
- Secrets via env vars or `.env`; document rotation.

### Expected artifacts (repo)
- `README.md` (setup & run basics)
- `PROJECT_STRUCTURE.md` (initial)
- `Architecture.md` (high-level)
- `docker-compose.yml` (Postgres, RabbitMQ, Jaeger)
- `src` skeleton, `pom.xml` or `build.gradle`
- Flyway migrations `V1__init.sql`
- `OBSERVABILITY.md` (initial)
- `TESTING_STRATEGY.md` (initial)
- `CHAT_HISTORY.md` (start log)
- `API-SPECIFICATION.yml` (stub)
- `postman_collection.json` (minimal Postman collection for evaluator testing)

---

## Phase 2 — Core Payment Flows

### Objectives
- Implement end-to-end payment flows with Authorize.Net Sandbox: purchase (auth+capture), authorize→capture, void, refunds (full & partial), subscriptions, robust idempotency, and webhook queue-based processing.

### Backend modules (to implement)
- `gateway` — Authorize.Net adapter (SDK wrapper), request/response mapping, signature verification, response sanitization.
- `service` — `PaymentService`, `SubscriptionService`, `WebhookProcessingService` orchestration.
- `queue` — AMQP publisher/listener, message converters, dead-letter handling.
- `retry` — Resilience4j configuration (retry/backoff/circuit-breaker).
- `scheduler` — scheduled billing, reconciliation jobs and retry supervisors.
- `dto` — detailed request/response DTOs for payment endpoints.

### Internal domain events (new)
Under Phase 2 we define and emit internal domain events for asynchronous processing, observability and loose coupling. Example event types and payload notes:
- `PaymentAuthorizedEvent` — emitted after a successful authorization; includes `orderId`, `authorizationId`, `amount_cents`, `currency`, `timestamp`, `correlationId`.
- `PaymentCapturedEvent` — emitted after capture; includes `orderId`, `captureId`, `transaction_id`, `amount_cents`, `currency`, `settlement_time`, `correlationId`.
- `RefundProcessedEvent` — emitted after refund; includes `refundId`, `originalTransactionId`, `amount_cents`, `status`, `correlationId`.
- `SubscriptionChargedEvent` — emitted after scheduled subscription billing attempt; includes `subscriptionId`, `transactionId` (if created), `amount_cents`, `status`, `correlationId`.
- `WebhookReceivedEvent` — emitted on validated webhook receipt (before processing) to allow async consumption (includes `event_id`, `event_type`, `payload`, `received_at`, `correlationId`).

Events will be published internally via the message broker (RabbitMQ) or an in-memory event bus (for same-process listeners), and will be versioned (e.g., include `eventSchemaVersion`). Consumers should be resilient to idempotent re-delivery.

### Order state machine (explicit)
Orders progress through a well-defined state machine. State transitions must be persisted transactionally and audited.

- States:
  - `PENDING` — initial state after order creation, not yet authorized.
  - `AUTHORIZED` — authorization succeeded (holds funds) but not captured.
  - `CAPTURED` — capture succeeded; funds settled or in settlement pipeline.
  - `VOIDED` — auth was voided before settlement.
  - `REFUNDED` — a refund succeeded against a captured transaction (partial or full); may reference original capture.
  - `FAILED` — terminal error occurred while attempting a payment operation.
  - `CANCELLED` — order was cancelled by business logic before authorization.

- Transition rules examples:
  - `PENDING` -> `AUTHORIZED` on successful auth.
  - `AUTHORIZED` -> `CAPTURED` on successful capture.
  - `AUTHORIZED` -> `VOIDED` on successful void.
  - `CAPTURED` -> `REFUNDED` on successful refund.
  - Any state -> `FAILED` on unhandled gateway error (with reason code stored).
  - `PENDING` -> `CANCELLED` on explicit cancellation before auth.

Order entity `status` field must be constrained to these values; transitions must use optimistic locking (version column) to prevent races (double-capture). Each transition writes an `audit_logs` entry and optionally emits a domain event.

### Gateway response storage and redaction (clarification)
Storage of gateway responses must avoid sensitive cardholder data. Store only safe gateway fields and a redacted representation of the original gateway payload where necessary.

- Allowed fields to persist (example schema):
  - `transaction_id` (gateway txn id)
  - `auth_code`
  - `avs_result_code`
  - `cvv_result_code`
  - `response_code`
  - `settlement_time` (timestamp)
  - `message_code` / `message_text` (non-sensitive)

- Persistent `gateway_response` JSON should be normalized to keep only the allowed fields above and a `redacted` flag. Do NOT store PAN, cardholder name, CVV, or full raw gateway responses that include sensitive fields. Store `last4` and `token_id` separately when needed.

### Database tables / entities (added/modified)
- `subscriptions` — id, customer_id, authorize_net_subscription_id, plan_code, amount_cents, cadence, status, next_billing_at, metadata.
- `refunds` or extend `transactions` to track refund info (original_txn_id, refund_amount_cents).
- `transactions` extended with `parent_transaction_id`, `gateway_response_code`, `settlement_time`, `gateway_response` (redacted JSON per rules above).
- `idempotency_keys` extended with `expires_at`, `consumed_at` (see idempotency expiry rules below).
- `webhook_events` extended with `signature_valid`, `dlq_reason`.

### Idempotency key rules (explicit)
- All mutating endpoints must accept an `Idempotency-Key` header. Keys are recorded in `idempotency_keys` with `created_at` and `expires_at`.
- Default expiration: idempotency keys automatically expire after 24 hours (configurable per operation). After expiry, the same key is treated as new.
- For long-running operations (e.g., subscriptions), operations may require longer TTLs—this should be configurable.

Phase 3 will include a periodic cleanup job to remove expired idempotency keys and optionally archive their response snapshots after a retention period.

### API endpoints (Phase 2)
- `POST /api/v1/payments/purchase` — purchase (auth+capture)
- `POST /api/v1/payments/authorize` — auth-only
- `POST /api/v1/payments/capture` — capture for a prior auth
- `POST /api/v1/payments/void` — cancel/void pre-settlement
- `POST /api/v1/payments/refund` — refund (full/partial)
- `POST /api/v1/subscriptions` — create subscription
- `GET /api/v1/subscriptions/{id}` — subscription details
- `POST /api/v1/webhooks/authorize-net` — webhook receiver (validates signature; enqueues event)
- Admin: `POST /api/v1/admin/webhooks/replay`, `GET /api/v1/admin/transactions/{id}/events`

### Reconciliation job (new)
- Add a scheduled reconciliation job that runs periodically to sync subscription statuses and settled transactions with Authorize.Net. Purpose:
  - Detect missed or delayed webhook events.
  - Reconcile settlement times and amounts.
  - Mark transactions as `CAPTURED`/`REFUNDED` or queue failed reconciliations for operator review.
- Reconciliation should be idempotent, rate-limited, and paginated. It should emit reconciliation metrics and create audit entries for detected divergences.

### Data flow & sequence (text)
- Purchase flow:
  1. Client -> POST `/api/v1/payments/purchase` (JWT + `Idempotency-Key`)
  2. Controller -> Idempotency check -> PaymentService
  3. PaymentService -> persist order(PENDING) -> call `AuthorizeNetGateway.charge()` (Resilience4j)
  4. Gateway -> Authorize.Net Sandbox -> response (sanitize and persist allowed gateway fields)
  5. PaymentService -> persist transaction (PURCHASE) -> update order state -> emit `PaymentCapturedEvent` or `PaymentAuthorizedEvent` -> respond to client

- Webhook flow:
  1. Authorize.Net -> POST `/api/v1/webhooks/authorize-net`
  2. Controller validates signature -> emit `WebhookReceivedEvent` and enqueue to RabbitMQ
  3. Worker consumes -> idempotency check -> update transactions/subscriptions -> persist webhook event -> ack or DLQ

### Error-handling strategy
- Classify errors: transient vs permanent.
- Retriable transient gateway errors handled by Resilience4j (exponential backoff).
- Duplicate idempotent requests return prior response or 409 with snapshot.
- Webhook processing failures retry up to N attempts then move to DLQ.
- Use DB transactions + optimistic locking to prevent race conditions (double-capture).

### Security layers
- Webhook signature verification.
- Idempotency-Key enforcement.
- Role-based access for admin endpoints.
- Secrets: Authorize.Net keys via env vars; rotateable.

### Testing strategy (Phase 2)
- Unit tests for `PaymentService`, `SubscriptionService`, and `AuthorizeNetGateway` (mocks).
- Integration tests via Testcontainers for Postgres and RabbitMQ; gateway mocked or sandbox used selectively.
- E2E tests (manual/CI-optional) for full flows including reconciliation.

### Compliance considerations
- Tokenization only; store `token_id` and `last4` only.
- Audit trail for all state changes.
- Redact sensitive webhook payload fields before persistence.

### Expected artifacts (repo)
- Implemented source for `gateway` and `service` modules.
- Flyway migrations `V2__subscriptions.sql`, `V3__transactions_updates.sql`.
- `postman_collection.json` (minimal + extended Postman collection for evaluator testing) and fully updated `API-SPECIFICATION.yml` with payment endpoints.
- Integration tests and fixtures.
- `Architecture.md` with detailed flow diagrams/PlantUML (text-based) and DB schema.
- `PROJECT_STRUCTURE.md` updated.

---

## Phase 3 — Observability, Testing, Final Integrations & Documentation

### Objectives
- Finalize observability (traces, metrics, logs), reach ≥80% test coverage, add CI, finalize security/compliance docs, and produce final deliverables.

### Backend modules (to implement/enhance)
- `observability` — OpenTelemetry config, Micrometer metrics, custom business metrics, SLO monitoring.
- `logging` — JSON logging config, redaction helpers, log enrichment.
- `audit` — admin APIs to query/export audit logs.
- `ci` — GitHub Actions workflow and CI helper scripts.
- `performance` — load-testing harness examples (k6/JMeter sample scripts).

### SLA / Performance expectations (new)
Under observability, define basic operational SLAs and targets to guide alerting and capacity planning:
- P99 latency for payment API operations: < 500ms (under normal load)
- Retry success rate (transient gateway retries succeeding): > 98%
- Idempotency duplicates observed in production: < 1% of requests (monitor `idempotency.duplicates.count`)
- Webhook ingestion pipeline processing latency (receive → processed): P95 < 5s, P99 < 30s

These targets should be tracked via Micrometer metrics and surfaced in dashboards; breaches should generate alerts.

### Database tables / entities (added/modified)
- Enhance `audit_logs` with retention metadata and indexes.
- Optional `metrics_snapshots` for offline analysis (documented, optional implementation).

### API endpoints (Phase 3)
- Admin observability endpoints (secured):
  - `GET /api/v1/admin/audit-logs` — query audit logs
  - `GET /api/v1/admin/webhooks/dlq` — list DLQ entries
- Ensure `/actuator/prometheus` available for scraping (secured appropriately for prod)

### Reconciliation cleanup & maintenance (Phase 3 additions)
- Implement a cleanup job to purge expired idempotency keys after their TTL (24h default) and archive response snapshots older than a retention period (e.g., 90 days). Provide an option to run as a maintenance task or via an admin API.

### Data flow & sequence (text)
- Trace-propagation example (purchase + webhook reconciliation):
  1. Client -> POST `/api/v1/payments/purchase` (correlationId C)
  2. Service creates OpenTelemetry span `purchase.op` and calls gateway (span `gateway.call`)
  3. After persist & enqueue, worker consumes event with propagated trace headers and continues the trace
  4. Webhook arrives later and is reconciled under the same correlation/trace identifiers where possible

### Error-handling strategy
- All errors emit metrics and structured logs with `error_type`, `retryable`, `operation`, and `correlationId`.
- CI gating: failing unit tests or coverage <80% fails the CI job.
- Alerting recommendations: pay attention to DLQ spikes, high retry rates, gateway error rates.

### Security layers
- Recommend RS256 JWT with key rotation using Vault or cloud KMS in prod.
- Ensure actuator endpoints require admin roles or IP restrictions.
- Enforce TLS in production; doc how to run locally with dev certs.

### Testing strategy (Phase 3)
- Expand unit tests to cover branches and edge cases; increase coverage to ≥80%.
- Integration tests with Testcontainers; mark heavy integration tests as optional in CI if runners don't support Docker.
- Add contract tests (Newman / OpenAPI-based) to validate API spec.

### Compliance considerations
- Full PCI DSS guidance: scope reduction via tokenization, encrypted secrets, TLS, minimal logging of card data, retention & deletion policy, and audit log retention.
- Document how to move service into PCI scope if necessary and responsibilities for ASV scans.

### Expected artifacts (repo)
- `.github/workflows/ci.yml` — build/test/coverage pipeline.
- Final `README.md` with run/build/sandbox instructions.
- Final `PROJECT_STRUCTURE.md`, `Architecture.md`, `OBSERVABILITY.md` (metrics/tracing/logging), `API-SPECIFICATION.yml` and `postman_collection.json`.
- `TESTING_STRATEGY.md` and `TEST_REPORT.md` with JaCoCo coverage (≥80%).
- Finalized `CHAT_HISTORY.md` capturing design decisions.

---

## Cross-Phase Concerns

- Idempotency: central DB-backed idempotency enforcement for all mutating endpoints, with default TTL = 24 hours and a configurable expiry per operation. Expired keys are cleaned up by a scheduled maintenance job in Phase 3.
- Audit & Logging: Every state change writes `audit_logs`; logs are structured JSON and redact sensitive data according to the gateway response storage rules.
- Tracing & Metrics: Propagate `X-Correlation-ID` using the canonical format `app-{uuid}-{timestamp}` and OpenTelemetry traces; instrument key spans and metrics described earlier.
- Secrets: Use env vars or `.env` for local dev; recommend Vault/KMS for production secrets management.
- CI & Testcontainers: If CI cannot run Docker-based integration tests, provide instructions to run them locally and run fast unit tests in CI. Use separate Maven/Gradle profiles to split unit vs integration tests.

---

## Minimal metric list (to include in `OBSERVABILITY.md`)
- `payments.request.count` (labels: operation, status)
- `payments.latency` histogram (operation)
- `payments.gateway.errors` (error_type)
- `webhooks.received.count`, `webhooks.processed.success`, `webhooks.processed.failure`, `webhooks.dlq.count`
- `idempotency.duplicates.count`
- `subscriptions.due.count`, `subscriptions.failed.count`

---

## Deliverable checklist (final)
- Source code implementing modules above
- `README.md`, `PROJECT_STRUCTURE.md`, `Architecture.md`, `OBSERVABILITY.md`
- `API-SPECIFICATION.yml` and `postman_collection.json` (minimal + extended collections for evaluator testing)
- `docker-compose.yml` (dev stack)
- `CHAT_HISTORY.md`, `TESTING_STRATEGY.md`, `TEST_REPORT.md`
- Flyway migrations and DB schema scripts
- Unit & integration tests with JaCoCo coverage >=80%
- `.github/workflows/ci.yml`

---

If you want this blueprint updated (e.g., change DB choice, replace RabbitMQ with Kafka, or prefer Java 21), tell me and I will update the document. Next step (optional): scaffold Phase 1 files, add Makefile helpers, and a CI skeleton.

---

_Generated on: 2025-12-06_
