package com.example.payment.events;

import java.time.Instant;

public class WebhookReceivedEvent {
    private final String webhookId;
    private final String source;
    private final String payload;
    private final String correlationId;
    private final Instant timestamp;
    private final String eventSchemaVersion;

    public WebhookReceivedEvent(String webhookId, String source, String payload, String correlationId, Instant timestamp, String eventSchemaVersion) {
        this.webhookId = webhookId;
        this.source = source;
        this.payload = payload;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.eventSchemaVersion = eventSchemaVersion;
    }

    public String getWebhookId() { return webhookId; }
    public String getSource() { return source; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public String getEventSchemaVersion() { return eventSchemaVersion; }
}
