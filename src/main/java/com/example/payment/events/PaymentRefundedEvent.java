package com.example.payment.events;

import java.time.Instant;

public class PaymentRefundedEvent {
    private final String orderId;
    private final String transactionId;
    private final Long amountCents;
    private final String correlationId;
    private final Instant timestamp;
    private final String eventSchemaVersion;

    public PaymentRefundedEvent(String orderId, String transactionId, Long amountCents, String correlationId, Instant timestamp, String eventSchemaVersion) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amountCents = amountCents;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.eventSchemaVersion = eventSchemaVersion;
    }

    public String getOrderId() { return orderId; }
    public String getTransactionId() { return transactionId; }
    public Long getAmountCents() { return amountCents; }
    public String getCorrelationId() { return correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventSchemaVersion() { return eventSchemaVersion; }
}
