# Testing Strategy — Springboot Payment Orchestrator

This document describes the recommended testing strategy for the payment orchestrator project. It covers scope, mocking approaches, coverage goals, and targeted test types (webhook, idempotency, DB transaction, API contract). The goal is repeatable, fast unit tests plus representative integration tests using Testcontainers to validate behavior against real infra (Postgres, RabbitMQ) when needed.

## Goals
- Ensure core business logic (payments, idempotency, subscriptions, webhook handling) is correct and resilient.
- Prevent regressions by enforcing coverage and CI gates.
- Validate interactions with external systems (gateway, queue, DB) through controlled integration tests.
- Provide deterministic tests for async and failure scenarios (retries, DLQ behavior).

## Test Pyramid and Scope
- Unit Tests (fast, many): single-class / single-component tests using JUnit 5 + Mockito. Focus on service logic, validators, mappers, and small helpers.
- Integration Tests (medium): Spring Boot tests that wire multiple components (controller -> service -> repository) but mock external HTTP calls (gateway) with WireMock or use Testcontainers for gateway simulation.
- End-to-end / Acceptance Tests (few, slow): Real services via Testcontainers (Postgres, RabbitMQ) or a local docker-compose stack. Validate full flows: purchase → webhook → ledger persistence.

## Unit Testing Scope
- Service layer: Payment orchestration flows (purchase, authorize, capture, void, refund, subscribe/unsubscribe).
- Domain logic: idempotency resolution, state transitions (AUTHORIZED→CAPTURED→REFUNDED), amount validation, rounding rules.
- Security: JWT parsing/validation helpers and permission checks (unit-tests for edge cases).
- Utilities: Correlation id propagation, audit event creation, DTO <-> entity mapping.
- Error mapping: exceptions -> API errors mapping logic.

Keep unit tests focused on behavior; avoid testing Spring plumbing in unit tests.

## Mocking Strategy
- Use Mockito (or Mockito-Kotlin if applicable) for core unit tests to mock repositories, outbound gateway adapters, and messaging templates.
- Use WireMock for HTTP-based gateways (Authorize.Net simulator) when you need to assert HTTP request content and return controlled gateway responses.
- Use Testcontainers when you need a real database or broker (Postgres, RabbitMQ) to validate transactions, locking, and message delivery semantics.
- Prefer mocking gateway client libraries rather than raw HTTP when the SDK is used — mock the SDK interface if it is wrapped by an adapter/gateway class.
- Keep external interaction behind clear gateway adapters; tests mock adapters rather than business logic.

## Test Coverage Goals
- Target: >= 80% line coverage project-wide.
- Critical packages (payment flow, idempotency, gateway adapters): >= 90% lines covered.
- Use JaCoCo for coverage measurement and fail build when thresholds are not met. Suggested Maven config:

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.8</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

Enforce thresholds with the `check` goal or via CI checks (SonarQube, Codecov).

## Webhook Tests
Unit-level:
- Validate signature verification helper with positive and negative fixtures (valid signature, malformed signature, missing header).
- Test payload parsing and minimal validation logic.

Integration-level:
- Use WireMock or local HTTP client to POST example webhook payloads to the `/webhooks/authorize-net` endpoint.
- For full flow: run the application with Testcontainers (DB + RabbitMQ) and submit webhook POSTs; assert that events are enqueued (check RabbitMQ queue) and that the expected DB state change or audit log record is created.

Edge cases:
- Duplicate webhook deliveries — verify idempotent processing (webhook id or signature + event_id dedup store triggers deduplication).
- Invalid signature returns `400` or `403` and is not enqueued.

Tools/approaches:
- Use WireMock for gateway-origin tests; use Testcontainers for end-to-end webhook intake tests.

## Idempotency Tests
Unit tests:
- Test idempotency key parsing and short-circuit behavior in the service layer: when a request with a known idempotency key is received, return stored response or raise appropriate conflict.

Integration / Concurrency tests:
- Simulate concurrent requests with the same `Idempotency-Key` header to the purchase/authorize endpoints.
- Use a `ExecutorService` in tests to submit N concurrent HTTP requests (RestAssured or MockMvc) carrying the same idempotency key. Expect only one successful external charge call and subsequent calls returning the stored result or `409` depending on policy.
- Validate DB uniqueness constraint on an idempotency table (unique(idempotency_key, principal)) and test for graceful handling of constraint violation.

Example approach (pseudo-code):

```java
// Using Testcontainers + RestAssured
ExecutorService e = Executors.newFixedThreadPool(8);
List<Future<Response>> futures = IntStream.range(0,8)
  .mapToObj(i -> e.submit(() -> given().header("Idempotency-Key", "key-123")...post(...)))
  .collect(...);
// collect and assert that exactly one created/charged request reached the gateway mock
```

Consider adding a contract for idempotency behavior in API documentation (expected statuses on duplicate requests).

## DB Transaction Tests
- Use Testcontainers to run a real Postgres instance during integration tests.
- Mark integration tests that require DB as a separate profile (e.g., `-Pit` or `-Pintegration-tests`) so they don't run on every `mvn test` by default.
- Validate transactional boundaries:
  - Tests should assert that failed flows roll back DB changes (use `@Transactional` for rollback semantics in integration tests).
  - Test optimistic locking behavior by updating the same entity in two transactions (version mismatch causes an exception and proper retry handling).
  - Verify idempotency store entries are persisted atomically with the business write to avoid partial side effects.

Example patterns:
- Use `@SpringBootTest` with `Testcontainers` and `@ActiveProfiles("test")`.
- Use `JdbcTemplate` or repository beans inside tests to assert the DB state after operations.

## API Contract Tests
- Consumer-driven contract: use Pact (for consumers that call this service) or publish provider verifier tests.
- OpenAPI-based validation: run schema validation tests that assert the API responses conform to `API-SPECIFICATION.yml`. Options:
  - Use `openapi4j`/`swagger-request-validator` to validate responses in automated tests.
  - Use Postman + Newman to run request/response validation against the generated `postman_collection.json`.
  - Use Spring Cloud Contract if you want automatic stubs and contract publishing.

Sample approaches:
- Automated OpenAPI validation in tests:
  - Load `API-SPECIFICATION.yml` in a test, make an HTTP call (RestAssured) to the running app, and validate the response matches the openapi schema.
- Postman/Newman:
  - Use the generated `postman_collection.json` in CI with `newman run postman_collection.json` to validate endpoints quickly.

## Tools & Libraries (recommended)
- JUnit 5 (JUnit Jupiter) — unit & integration tests.
- Mockito (with inline mock maker where needed) — mocking dependencies.
- Spring Boot Test (spring-boot-starter-test) — test slices, MockMvc, test configuration.
- Testcontainers — Postgres, RabbitMQ containers for integration tests.
- WireMock — mock external HTTP gateway endpoints.
- RestAssured — fluent API testing for HTTP endpoints.
- AssertJ — expressive assertions.
- Awaitility — wait/assert on async conditions (queues, eventual DB state).
- Jacoco — coverage reports and thresholds.
- Pact or Spring Cloud Contract — contract testing.
- Newman (Postman CLI) — run `postman_collection.json` in CI if desired.

Suggested dependency coordinates (examples):

```xml
<!-- junit + mockito are included in spring-boot-starter-test; add Testcontainers and WireMock -->
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <version>1.18.3</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>rabbitmq</artifactId>
  <version>1.18.3</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.github.tomakehurst</groupId>
  <artifactId>wiremock-jre8</artifactId>
  <version>2.35.0</version>
  <scope>test</scope>
</dependency>
```

## CI Integration and Execution
- Split tests into fast unit tests (run on every push) and slower integration tests (run nightly or on PR with `integration` profile).
- CI pipeline steps:
  1. `mvn -DskipITs=true verify` — run unit tests and static checks.
  2. `mvn -Pit verify` — run integration tests (Testcontainers) on designated CI agents with Docker available.
  3. Upload Jacoco/coverage report to Codecov or SonarQube.

Example commands:

```powershell
mvn -DskipITs=true clean test
# Integration / Testcontainers (requires Docker)
mvn -Pit -DskipTests=false verify
```

## Test Data and Fixtures
- Use factory builders or fixture builders (e.g., `TestDataBuilder`) to construct domain objects consistently.
- Keep example webhook payloads and gateway responses under `src/test/resources/fixtures`.
- Use Flyway migrations to bring DB schema to known state before integration tests. Optionally apply test-specific SQL fixtures during `@BeforeAll`.

## Performance & Load Tests (optional)
- For idempotency / concurrency validation, run targeted load tests (e.g., Gatling or JMeter) against a staging environment to validate behavior under high concurrency.

## Reporting & Debugging Tips
- Capture request/response payloads in test logs for failing integration tests (redact secrets).
- Persist failing test DB dumps or container logs as CI artifacts for triage.

## Example test checklist (per pull request)
- Unit tests added for any new public method in service layer.
- New gateway interactions have WireMock unit/integration stubs.
- Idempotency behavior covered by at least one unit + one integration test.
- New DB interactions covered by an integration test using Testcontainers when changes affect transactional semantics.
- Jacoco coverage for the changed modules meets the threshold.

---

If you want, I can:
- Add starter test classes to the repository for a sample payment flow, idempotency concurrency test, and a webhook integration test (with Testcontainers and WireMock). 
- Add Maven/Gradle configuration to separate unit vs integration test phases and a sample GitHub Actions workflow to run both.

Which of these would you like me to implement next? 
