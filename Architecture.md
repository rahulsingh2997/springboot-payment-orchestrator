# Architecture

This project is a modular Spring Boot orchestration service for payments. Key elements:

- API Layer: REST controllers exposing payment and subscription endpoints.
- Service Layer: orchestrates business flows (authorize, capture, refund, subscription lifecycle).
- Integration Layer: wrappers for Authorize.Net SDK and external HTTP clients.
- Persistence: Spring Data JPA, PostgreSQL, Flyway for migrations.
- Messaging: RabbitMQ used for webhook/event processing and async reconciliation.
- Observability: OpenTelemetry tracing (Jaeger), Micrometer metrics (Prometheus), structured logs.
- Security: JWT-based auth for dev/test; production should use OIDC and secrets management.

Design goals:
- Small surface area for testing and CI.
- Deterministic flows with idempotency keys and event-driven reconciliation.
- Strong observability and auditability for financial flows.
# Architecture & API Overview

## System Overview
The service is a modular Spring Boot application that orchestrates payments via the Authorize.Net SDK. It supports:
- Purchase (auth + capture)
- Two-step authorize → capture
- Void/Cancel before capture
- Refunds (full & partial)
- Subscriptions / Recurring Billing
- Webhook receiver + queue-based processing for async events
- Idempotency via `Idempotency-Key` header
- JWT authentication for API endpoints

## API Endpoints (high level)
- `POST /api/v1/auth/dev/token` — (dev) mint JWT for local testing
- `POST /api/v1/orders` — create order
- `GET /api/v1/orders/{orderId}` — get order
- `POST /api/v1/payments/purchase` — purchase (auth+capture)
- `POST /api/v1/payments/authorize` — authorize only
- `POST /api/v1/payments/capture` — capture authorization
- `POST /api/v1/payments/void` — void an authorization
- `POST /api/v1/payments/refund` — refund a captured transaction
- `POST /webhooks/authorize-net` — webhook receiver (validates signature and enqueues)

## Data model (summary)
- `Order` — externalOrderId, customerId, amount, currency, status
- `Transaction` — transactionId, orderId, type (AUTH, CAPTURE, REFUND, VOID), amount, status, gatewayResponse
- `Subscription` — subscriptionId, plan, status, nextChargeAt
- `WebhookEvent` — raw payload, processed flag, receivedAt

## Design trade-offs
- Monolith vs microservices: chosen modular monolith to reduce operational complexity for the exercise.
- Sync vs async: synchronous flows for immediate payment operations; asynchronous queue-based processing for webhooks and reconciliation to improve throughput and crash resilience.
- Secrets: dev uses env vars; production must use Vault/KMS and rotated keys.

## Security & Compliance
- Do not log PAN/CVV. Only store `card_last4`, `card_brand`, `payment_token`.
- JWT authentication for control of endpoints
- For PCI scope reduction, prefer tokenization (client-side tokenization) rather than direct PAN handling.

## Observability
- Metrics exposed via `/actuator/prometheus`
- Traces exported to Jaeger via OpenTelemetry exporter
- Correlation via `X-Correlation-ID`

