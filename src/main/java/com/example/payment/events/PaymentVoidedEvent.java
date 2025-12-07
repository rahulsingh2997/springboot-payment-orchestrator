package com.example.payment.events;

import java.time.Instant;

public class PaymentVoidedEvent {
    private final String orderId;
    private final String transactionId;
    private final String correlationId;
    private final Instant timestamp;
    private final String eventSchemaVersion;

    public PaymentVoidedEvent(String orderId, String transactionId, String correlationId, Instant timestamp, String eventSchemaVersion) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.eventSchemaVersion = eventSchemaVersion;
    }

    public String getOrderId() { return orderId; }
    public String getTransactionId() { return transactionId; }
    public String getCorrelationId() { return correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventSchemaVersion() { return eventSchemaVersion; }
}
