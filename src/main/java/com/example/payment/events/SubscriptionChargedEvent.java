package com.example.payment.events;

import java.time.Instant;

public class SubscriptionChargedEvent {
    private final String subscriptionId;
    private final Long amountCents;
    private final String correlationId;
    private final Instant timestamp;
    private final String eventSchemaVersion;

    public SubscriptionChargedEvent(String subscriptionId, Long amountCents, String correlationId, Instant timestamp, String eventSchemaVersion) {
        this.subscriptionId = subscriptionId;
        this.amountCents = amountCents;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.eventSchemaVersion = eventSchemaVersion;
    }

    public String getSubscriptionId() { return subscriptionId; }
    public Long getAmountCents() { return amountCents; }
    public String getCorrelationId() { return correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventSchemaVersion() { return eventSchemaVersion; }
}
