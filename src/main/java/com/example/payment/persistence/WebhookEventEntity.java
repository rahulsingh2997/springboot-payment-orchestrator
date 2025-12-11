package com.example.payment.persistence;

import com.example.payment.persistence.enums.WebhookEventStatus;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "webhook_events")
public class WebhookEventEntity {

    @Id
    private String id;

    @Column(name = "source")
    private String source;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WebhookEventStatus status;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public WebhookEventStatus getStatus() { return status; }
    public void setStatus(WebhookEventStatus status) { this.status = status; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
