# Observability

This project includes tracing, metrics, and structured logs:

- Tracing: OpenTelemetry with Jaeger exporter. Default local Jaeger is started via `docker-compose`.
- Metrics: Micrometer exposing `/actuator/prometheus` (configure Prometheus to scrape this endpoint).
- Logs: Structured JSON logging recommended for ingestion into ELK/Logstash.

How to view locally:

1. Start `docker-compose up -d` to bring up Jaeger and the application.
2. Visit Jaeger UI (default: `http://localhost:16686`) to view traces.
3. Visit Prometheus/Grafana if configured to see metrics dashboards.

Recommendation: In CI, ensure traces/metrics are optional; CI focuses on unit/integration tests and JaCoCo coverage.
# Observability

This document summarizes metrics, tracing and logging strategies used by the service.

Metrics
- Expose `GET /actuator/prometheus` for Prometheus scraping.
- Key metrics:
  - `payment_authorized_total` (counter)
  - `payment_captured_total` (counter)
  - `payment_refunded_total` (counter)
  - `payment_voided_total` (counter)
  - `webhook_received_total` (counter)
  - `webhook_processed_total` (counter)
  - `subscription_created_total` (counter)
  - `subscription_renewed_total` (counter)

Tracing
- Each incoming request should include `X-Correlation-ID`; if absent a unique UUID is generated.
- OpenTelemetry SDK configured to export to Jaeger (see `application-docker.yml`).
- Span naming: `http.controller.{ControllerName}.{operation}` and downstream spans for gateway calls.

Logging
- Structured JSON logs via Logstash encoder.
- Include `timestamp`, `level`, `correlationId`, `traceId`, `spanId`, `event`, and `message`.
- Audit logs (write-only) for payment actions in `audit_log` table with user and request metadata.

Alerting (recommended)
- Increase in `payment_voided_total` or `payment_refunded_total` over baseline -> investigate possible fraud or gateway failures.
- Elevated 5xx rates on API endpoints -> investigate service errors.

Visualization
- Use Grafana to plot the Prometheus metrics; optionally create dashboards for payments, webhooks, and subscriptions.

