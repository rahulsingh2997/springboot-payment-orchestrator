**API Summary — Springboot Payment Orchestrator**

This file summarizes the primary API endpoints with example curl commands and sample request/response payloads. Use `API-SPECIFICATION.yml` for the canonical OpenAPI definition.

- **Auth**: All endpoints (except webhook) require a JWT bearer token in `Authorization: Bearer <token>` header.
- **Tracing**: Provide `X-Correlation-ID` header (UUID) for tracing. Server will also return it in response payloads.

1) Purchase
- Method: POST
- Path: `/api/payments/purchase`
- Description: Performs authorization and capture (purchase) when `capture=true`. If `capture=false`, performs auth-only.
- Example curl:

```bash
curl -X POST "https://api.example.local/api/payments/purchase" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: 11111111-2222-3333-4444-555555555555" \
  -H "Content-Type: application/json" \
  -d '{"order_id":"ORD-1001","payment_method_token":"tok_abc123","amount":{"amount_cents":4999,"currency":"USD"},"capture":true}'
```

- Sample success response (200):

```json
{
  "order_id": "ORD-1001",
  "transaction_id": "txn_abc123",
  "status": "CAPTURED",
  "amount": { "amount_cents": 4999, "currency": "USD" },
  "correlation_id": "11111111-2222-3333-4444-555555555555"
}
```

2) Authorize
- Method: POST
- Path: `/api/payments/authorize`
- Example curl:

```bash
curl -X POST "https://api.example.local/api/payments/authorize" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: <uuid>" \
  -H "Content-Type: application/json" \
  -d '{"order_id":"ORD-2001","payment_method_token":"tok_def456","amount":{"amount_cents":2500,"currency":"USD"}}'
```

- Sample success response (200):

```json
{
  "authorization_id": "auth_456",
  "status": "AUTHORIZED",
  "amount": { "amount_cents": 2500, "currency": "USD" },
  "correlation_id": "<uuid>"
}
```

3) Capture
- Method: POST
- Path: `/api/payments/capture`
- Example curl:

```bash
curl -X POST "https://api.example.local/api/payments/capture" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: <uuid>" \
  -H "Content-Type: application/json" \
  -d '{"authorization_id":"auth_456","amount":{"amount_cents":2500,"currency":"USD"}}'
```

- Sample success response (200):

```json
{
  "capture_id": "cap_789",
  "status": "CAPTURED",
  "amount": { "amount_cents": 2500, "currency": "USD" },
  "correlation_id": "<uuid>"
}
```

4) Void (Cancel)
- Method: POST
- Path: `/api/payments/void`
- Example curl:

```bash
curl -X POST "https://api.example.local/api/payments/void" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: <uuid>" \
  -H "Content-Type: application/json" \
  -d '{"authorization_id":"auth_456","reason":"Customer cancellation"}'
```

- Sample success response (200):

```json
{
  "void_id": "void_012",
  "status": "VOIDED",
  "correlation_id": "<uuid>"
}
```

5) Refund
- Method: POST
- Path: `/api/payments/refund`
- Example curl:

```bash
curl -X POST "https://api.example.local/api/payments/refund" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: <uuid>" \
  -H "Content-Type: application/json" \
  -d '{"transaction_id":"txn_abc123","amount":{"amount_cents":1500,"currency":"USD"},"reason":"Customer returned item"}'
```

- Sample success response (200):

```json
{
  "refund_id": "ref_345",
  "status": "REFUNDED",
  "amount": { "amount_cents": 1500, "currency": "USD" },
  "correlation_id": "<uuid>"
}
```

6) Create Subscription
- Method: POST
- Path: `/api/subscriptions`
- Example curl:

```bash
curl -X POST "https://api.example.local/api/subscriptions" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: <uuid>" \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"cust_900","plan_code":"plan_monthly_basic","amount":{"amount_cents":999,"currency":"USD"},"cadence":"MONTHLY","trial_days":14}'
```

- Sample success response (201):

```json
{
  "subscription_id": "sub_678",
  "status": "ACTIVE",
  "next_billing_at": "2026-01-06T00:00:00Z",
  "correlation_id": "<uuid>"
}
```

7) Get Subscription
- Method: GET
- Path: `/api/subscriptions/{subscriptionId}`
- Example curl:

```bash
curl -X GET "https://api.example.local/api/subscriptions/sub_678" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Correlation-ID: <uuid>"
```

8) Webhook Listener (Authorize.Net)
- Method: POST
- Path: `/webhooks/authorize-net`
- Notes: Public endpoint for gateway webhooks. Validate gateway signature header and return `202 Accepted` when accepted.
- Example curl:

```bash
curl -X POST "https://api.example.local/webhooks/authorize-net" \
  -H "Content-Type: application/json" \
  -H "X-ANet-Signature: <signature>" \
  -H "X-Correlation-ID: <uuid>" \
  -d '{"event_id":"evt_987","event_type":"net.authorize.payment.authcapture.created","payload":{"transaction_id":"txn_abc123","status":"CAPTURED"}}'
```

Error handling common cases:
- 400 Bad Request: malformed JSON, missing required fields, invalid amounts.
- 401 Unauthorized: missing/invalid JWT when required.
- 403 Forbidden: signature validation failed for webhook (treat as 400 or 403 depending on policy).
- 404 Not Found: subscription or resource not found.
- 409 Conflict: idempotency conflict or duplicate order id.

If you'd like, I can generate a version of this summary that includes example HTTP responses for all error cases, add `Idempotency-Key` header examples, or export a Postman environment file.
