# Project Structure

Top-level layout and purpose of key folders/files:

- `src/main/java` — application source code (controllers, services, repositories, configs)
- `src/main/resources` — Spring Boot configuration and Flyway migrations (`db/migration`)
- `src/test` — unit and integration tests (JUnit 5, Testcontainers)
- `pom.xml` — Maven build file and plugin configuration (JaCoCo, Surefire)
- `Dockerfile` — container image for the Spring Boot app
- `docker-compose.yml` — local dev stack (Postgres, RabbitMQ, Jaeger, app)
- `scripts/` — helper scripts (verification, start/stop helpers)
- `logs/` — generated evidence and verification artifacts (MP12/MP13 logs)
- `target/` — Maven build output (generated at build time)
- `.github/workflows/ci.yml` — CI pipeline (build, test, jacoco upload)

Use this document to quickly locate components when preparing the submission.
# Project Structure

This document explains the main folders and files in the repository.

- `pom.xml` — Maven build definition and plugins (JaCoCo, Spring Boot plugin).
- `Dockerfile` — Reproducible Docker image build for the application.
- `docker-compose.yml` — Dev compose stack: `postgres`, `rabbitmq`, `jaeger`, and `app` service.
- `src/main/java` — Application Java source code.
  - `com.example.payment.api` — REST controllers and DTOs.
  - `com.example.payment.service` — Business orchestration code.
  - `com.example.payment.gateway` — Authorize.Net integration wrapper.
  - `com.example.payment.persistence` — JPA entities and repositories.
  - `com.example.payment.idempotency` — idempotency helpers.
  - `com.example.payment.observability` — tracing and metrics utilities.
- `src/main/resources` — Spring configuration, Flyway migrations (`db/migration`).
- `src/test/java` — Unit and integration tests (JUnit 5, Testcontainers).
- `scripts/` — verification scripts used for MP12/MP13 verification.
- `logs/` — generated verification artifacts and test logs (do not commit secrets).
- `.github/workflows/` — CI workflows (build, test, coverage).

Guidance: use this document to quickly orient contributors and reviewers to where to find code and artifacts.
