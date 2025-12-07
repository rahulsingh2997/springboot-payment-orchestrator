# springboot-payment-orchestrator

A robust Spring Boot backend that orchestrates payments through the Authorize.Net Sandbox. This repository contains the design, scaffolding, and (eventually) a production-capable implementation supporting core payment flows (purchase, authorize/capture, void, refund), recurring billing, idempotency, webhook handling, JWT authentication, tracing, observability, and audit logs.

---

## 1. Project Overview

`springboot-payment-orchestrator` is a backend service that integrates directly with the Authorize.Net Sandbox to provide a developer-friendly, auditable, and secure payment orchestration layer. The service exposes REST endpoints for payment operations, persists order and transaction history, enforces idempotency, and includes a queue-backed webhook processor for asynchronous events.

This repository is organized into three phased deliverables: Foundation (phase 1), Core Payment Flows (phase 2), and Observability/Testing & Final Integrations (phase 3). See `PROJECT_BLUEPRINT.md` for the full plan.

## 2. Vision & Requirements

- Support core payment flows: Purchase (auth+capture), Authorize → Capture, Cancel (void pre-capture), Refund (full + partial).
- Support subscriptions/recurring billing via Authorize.Net recurring APIs.
- Ensure safe retries and idempotency (Idempotency-Key support).
- Handle async events via webhooks and queue-based processing (RabbitMQ recommended for local/dev).
- Include distributed tracing (correlation IDs & OpenTelemetry), metrics, and structured logs.
- Secure internal public endpoints with JWT authentication.
- Persist orders, transactions, subscriptions, and audit logs in PostgreSQL.
- Unit tests and integration tests with JaCoCo coverage reporting (target ≥80%).

## 3. Architecture Summary

- Single Spring Boot application (modular monolith) with clear package boundaries:
	- API layer (controllers, DTOs, validation)
	- Service layer (business logic orchestration)
	- Gateway/integration layer (Authorize.Net SDK wrapper)
	- Persistence layer (Spring Data JPA + Flyway migrations)
	- Queue layer (RabbitMQ + Spring AMQP) for webhook processing
	- Observability (OpenTelemetry, Micrometer, Jaeger, Prometheus)
	- Security (Spring Security + JWT)

- Flow examples:
	- Purchase: API -> PaymentService -> Authorize.Net -> persist transaction -> update order -> emit events
	- Webhook: Authorize.Net -> webhook endpoint -> validate signature -> enqueue -> worker -> process & reconcile

## 4. Tech Stack

- Language & Framework: Java 17 (recommended) + Spring Boot 3.x
- Build: Maven (pom.xml)
- Persistence: PostgreSQL, Spring Data JPA, Flyway
- Messaging: RabbitMQ + Spring AMQP (dev queueing)
- Payment Gateway SDK: Authorize.Net official Java SDK (`anet-java-sdk`)
- Security: Spring Security + JWT (Nimbus JOSE / JJWT)
- Resilience: Resilience4j (retry/circuit-breaker)
- Observability: OpenTelemetry, Micrometer, Jaeger, Prometheus
- Testing: JUnit 5, Mockito, Testcontainers (Postgres, RabbitMQ), JaCoCo for coverage
- CI: GitHub Actions (recommended)

## 5. High-level API List

The exact API will be implemented in Phase 1/2. High-level endpoints (subject to small changes):

- Authentication
	- `POST /api/auth/login` — (dev) issue a JWT for testing (or integrate with external IdP)
- Orders & Payments
	- `POST /api/orders` — create order (initial)
	- `GET /api/orders/{orderId}` — fetch order
	- `POST /api/payments/purchase` — purchase (auth + capture)
	- `POST /api/payments/authorize` — authorize only
	- `POST /api/payments/capture` — capture an existing authorization
	- `POST /api/payments/void` — void/cancel before settlement
	- `POST /api/payments/refund` — refund (full/partial)
- Subscriptions
	- `POST /api/subscriptions` — create subscription
	- `GET /api/subscriptions/{id}` — subscription details
- Webhooks
	- `POST /webhooks/authorize-net` — webhook receiver (validates signature, enqueues)

Notes:
- All mutating endpoints support `Idempotency-Key` header. Include `X-Correlation-ID` header to trace requests across services.

## 6. Setup Instructions

Requirements:
- Java 17 (or 21 if you prefer) installed and `JAVA_HOME` configured.
- Maven 3.8+ installed.
- Docker & Docker Compose (for local Postgres, RabbitMQ, Jaeger, Prometheus) — required for full local stack.

### Java & Maven

Install Java 17 and Maven. For Windows PowerShell, example commands:

```powershell
# Verify Java
java -version
# Verify Maven
mvn -v
```

### Database setup (local with Docker Compose)

Docker Compose will start a local PostgreSQL instance for development. The repo includes a `docker-compose.yml` (Phase 1) that starts Postgres, RabbitMQ, and Jaeger.

Start the dev stack:

```powershell
docker-compose up -d
```

The default Postgres connection (example):

- Host: `localhost`
- Port: `5432`
- DB: `payments_db`
- User: `payments_user`
- Password: `payments_pass`

These are placeholders — the real values are configured via environment variables. Do NOT commit production credentials.

### Environment variables

Create a `.env` file or set environment variables in your shell. Minimal set for dev:

```text
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/payments_db
SPRING_DATASOURCE_USERNAME=payments_user
SPRING_DATASOURCE_PASSWORD=payments_pass
AUTHORIZE_NET_API_LOGIN_ID=<your-sandbox-api-login-id>
AUTHORIZE_NET_TRANSACTION_KEY=<your-sandbox-transaction-key>
AUTHORIZE_NET_ENV=sandbox
JWT_SECRET=<dev-jwt-secret> # for HS256 dev tokens
RABBITMQ_URL=amqp://guest:guest@localhost:5672/
```

Notes:
- In production, use Vault/KMS for secrets and RS256 keypairs for JWT.
- `AUTHORIZE_NET_API_LOGIN_ID` and `AUTHORIZE_NET_TRANSACTION_KEY` are obtained from your Authorize.Net sandbox account (see section 10).

### Running Docker Compose (dev)

```powershell
docker-compose up -d
# Verify services
docker-compose ps
```

If you modify Docker Compose, re-run `docker-compose up -d`.

## 7. Running the Application

From project root, run:

```powershell
mvn clean package -DskipTests
java -jar target/springboot-payment-orchestrator-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

Or run with Maven directly (dev):

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Application will start on default port `8080` unless changed in `application.yml`.

## 8. Running Unit Tests & Generating Coverage Report

Run unit tests and generate JaCoCo coverage report:

```powershell
mvn clean test
mvn jacoco:report

# Open report
start target/site/jacoco/index.html
```

CI will run tests and fail the build if coverage is below configured threshold (to be defined). Integration tests using Testcontainers require Docker available on CI runners and may be configured to run selectively.

## 9. Folder Structure

Initial (phase 1) expected structure:

```
springboot-payment-orchestrator/
├── README.md
├── PROJECT_BLUEPRINT.md
├── pom.xml
├── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── java/com/example/payment/...
│   │   └── resources/application.yml
│   └── test/
└── docs/
```

Final (phase 3) expected structure:

```
springboot-payment-orchestrator/
├── README.md
├── PROJECT_BLUEPRINT.md
├── PROJECT_STRUCTURE.md
├── Architecture.md
├── OBSERVABILITY.md
├── API-SPECIFICATION.yml (or POSTMAN_COLLECTION.json)
├── docker-compose.yml
├── .github/workflows/ci.yml
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/payment/api
│   │   ├── java/com/example/payment/service
│   │   ├── java/com/example/payment/gateway
│   │   ├── java/com/example/payment/persistence
│   │   ├── java/com/example/payment/idempotency
│   │   ├── java/com/example/payment/observability
│   │   └── resources/
│   └── test/
└── docs/
```

## 10. Notes on Authorize.Net Sandbox setup

1. Create a sandbox account:
	 - Sign up at https://developer.authorize.net (Sandbox account)
2. Obtain credentials:
	 - API Login ID
	 - Transaction Key
	 - Webhook Signing Key (for webhook signature verification)
3. Use the official Authorize.Net Java SDK (`anet-java-sdk`) and configure with Sandbox endpoints (the SDK handles this when `ENVIRONMENT.SANDBOX` is selected).
4. Tokenization & card data handling:
	 - Do NOT store PAN or CVV in the application or logs.
	 - Use Accept.js or the CIM/tokenization features if client-side collection is required.
	 - Persist only `payment_token`, `card_last4`, and `card_brand`.
5. Webhook configuration:
	 - Configure webhook endpoints in the sandbox merchant interface and use the signing key to validate incoming events.

## Additional Notes

- Idempotency: include `Idempotency-Key` header on mutating requests to avoid duplicate charges.
- Correlation: include `X-Correlation-ID` for tracing; if missing the service will generate one.
- Security: this README assumes development mode uses HS256 for JWT for convenience; rotate to RS256 and a proper key-management flow for production.

---

If you'd like, I can now scaffold Phase 1 (project skeleton, `pom.xml`, Flyway migrations, `docker-compose.yml`, and a minimal controller) once you confirm Java and message broker choices. Otherwise, I can produce an OpenAPI draft next.

## How to run the app locally (Master Prompt 6 Baseline)

This section describes the baseline verification steps for Master Prompt 6. The project at this stage includes a minimal, no-op payment gateway implementation and service/mapper stubs so the application can run locally for integration and idempotency testing.

1. Build the project (skip tests for a fast local check):

```powershell
mvn clean verify -DskipTests
```

2. Run the generated JAR with the `local` profile:

```powershell
java -jar target/*.jar --spring.profiles.active=local
```

3. Health check:

- URL: `GET http://localhost:8080/api/v1/health`
- Example (PowerShell):

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/health' -Method GET
```

4. Obtain a development JWT token (dev-only endpoint):

- URL: `POST http://localhost:8080/api/v1/auth/dev/token`
- Request body (JSON):

```json
{"username":"devuser","roles":["ROLE_USER"]}
```

- Example (PowerShell):

```powershell
$body = @{ username = 'devuser'; roles = @('ROLE_USER') } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/dev/token' -Method Post -Body $body -ContentType 'application/json'
```

5. Create an order with idempotency (example):

- URL: `POST http://localhost:8080/api/v1/orders`
- Required headers:
	- `Authorization: Bearer <token>` (use the token returned above)
	- `Idempotency-Key: <unique-key>`

- Example payload (JSON):

```json
{
	"externalOrderId": "ext-123",
	"customerId": "cust-1",
	"amountCents": 1500,
	"currency": "USD"
}
```

- Example (PowerShell):

```powershell
$token = '<paste-token-here>'
$body = @{ externalOrderId = 'ext-123'; customerId = 'cust-1'; amountCents = 1500; currency = 'USD' } | ConvertTo-Json -Compress
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/orders' -Method Post -Body $body -ContentType 'application/json' -Headers @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'my-key-1' }
```

6. Notes about the current baseline:

- A no-op payment gateway bean is configured in this baseline so the application starts without a production gateway implementation. This is intended for local testing of API wiring, idempotency, and persistence behavior.
- No changes to security rules, DB entities, or core business logic were made as part of this baseline — only service/gateway/mapper stubs and the idempotency TypeReference fix were added to enable runtime verification.

If you want, I can now create a lightweight README snippet showing cURL examples or commit these changes for you (I will make a single clean commit including the above files).

## Order Operations (Authorize, Capture, Void, Refund)

This service exposes operations to manage the lifecycle of payment authorizations and captures for `Order` resources. Each operation is a server-side action that calls the `PaymentService`/gateway and persists a `Transaction` record.

- **Authorize**: Reserve funds against a payment method (authorization only). Use this when you want to verify funds without capturing them immediately.
- **Capture**: Capture previously authorized funds. This converts an authorization into a settled charge (or instructs the gateway to capture the authorization).
- **Void**: Cancel an authorization before it is captured (pre-settlement). Use this to cancel an authorization to avoid capture.
- **Refund**: Return funds for a captured transaction (full or partial refund).

### Required headers

- `Authorization: Bearer <token>` — JWT dev token (use `POST /api/v1/auth/dev/token` to mint a local dev token).
- `Idempotency-Key: <unique-key>` — Required for mutating operations to support safe retries; supply a unique value per logical operation.
- Optional: `X-Correlation-ID: <uuid>` — helpful for tracing; the service will generate one if missing.

### Endpoint paths

- `POST /api/v1/orders/{orderId}/authorize` — authorize only
- `POST /api/v1/orders/{orderId}/capture` — capture an authorization
- `POST /api/v1/orders/{orderId}/void` — void an authorization (pre-capture)
- `POST /api/v1/orders/{orderId}/refund` — refund a captured transaction (JSON body may include `amountCents`)

---

### Examples — PowerShell (Invoke-RestMethod)

Replace `<token>`, `<orderId>` and `<idempotency-key>` before running.

Authorize (PowerShell):
```powershell
$headers = @{ Authorization = "Bearer <token>"; 'Idempotency-Key' = '<idempotency-key>' }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/orders/<orderId>/authorize" -Headers $headers
```

Capture (PowerShell):
```powershell
$headers = @{ Authorization = "Bearer <token>"; 'Idempotency-Key' = '<idempotency-key>' }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/orders/<orderId>/capture" -Headers $headers
```

Void (PowerShell):
```powershell
$headers = @{ Authorization = "Bearer <token>"; 'Idempotency-Key' = '<idempotency-key>' }
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/orders/<orderId>/void" -Headers $headers
```

Refund (PowerShell, with body):
```powershell
$headers = @{ Authorization = "Bearer <token>"; 'Idempotency-Key' = '<idempotency-key>' }
$body = @{ amountCents = 500 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/v1/orders/<orderId>/refund" -Body $body -Headers $headers -ContentType 'application/json'
```

### Examples — cURL equivalents

Authorize (cURL):
```bash
curl -X POST \
	-H "Authorization: Bearer <token>" \
	-H "Idempotency-Key: <idempotency-key>" \
	http://localhost:8080/api/v1/orders/<orderId>/authorize
```

Capture (cURL):
```bash
curl -X POST \
	-H "Authorization: Bearer <token>" \
	-H "Idempotency-Key: <idempotency-key>" \
	http://localhost:8080/api/v1/orders/<orderId>/capture
```

Void (cURL):
```bash
curl -X POST \
	-H "Authorization: Bearer <token>" \
	-H "Idempotency-Key: <idempotency-key>" \
	http://localhost:8080/api/v1/orders/<orderId>/void
```

Refund (cURL, with body):
```bash
curl -X POST \
	-H "Authorization: Bearer <token>" \
	-H "Idempotency-Key: <idempotency-key>" \
	-H "Content-Type: application/json" \
	-d '{"amountCents":500}' \
	http://localhost:8080/api/v1/orders/<orderId>/refund
```

---

### Expected JSON responses (examples)

The service returns an `OrderResponse` that includes the order state. For payment operations you will usually also want the latest transaction details. Example combined response snippets below show the common fields you can expect:

Authorize success example:
```json
{
	"transactionId": "auth-1234",
	"orderId": "2e7b6c3d-15e7-4b45-abcc-53d6bca542f7",
	"status": "AUTHORIZED",
	"correlationId": "6f6a1c2e-3b2d-4a1f-9f6a-0b7c8d9e0f1a"
}
```

Capture success example:
```json
{
	"transactionId": "cap-5678",
	"orderId": "2e7b6c3d-15e7-4b45-abcc-53d6bca542f7",
	"status": "CAPTURED",
	"correlationId": "6f6a1c2e-3b2d-4a1f-9f6a-0b7c8d9e0f1a"
}
```

Void success example:
```json
{
	"transactionId": "void-9012",
	"orderId": "2e7b6c3d-15e7-4b45-abcc-53d6bca542f7",
	"status": "VOIDED",
	"correlationId": "6f6a1c2e-3b2d-4a1f-9f6a-0b7c8d9e0f1a"
}
```

Refund success example:
```json
{
	"transactionId": "refund-3456",
	"orderId": "2e7b6c3d-15e7-4b45-abcc-53d6bca542f7",
	"status": "REFUNDED",
	"correlationId": "6f6a1c2e-3b2d-4a1f-9f6a-0b7c8d9e0f1a"
}
```

Note: actual responses may include additional order fields (amountCents, currency, timestamps) and transaction metadata depending on gateway responses and your configuration.

---

### Implementation note

Currently the `PaymentService` uses a no-op Authorize.Net gateway placeholder in local/dev mode so the application can run and the API wiring, idempotency, and persistence can be exercised without real sandbox credentials. The placeholder will be replaced with the full Authorize.Net sandbox implementation in a subsequent update.


