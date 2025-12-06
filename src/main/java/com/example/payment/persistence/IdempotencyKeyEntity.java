package com.example.payment.persistence;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "id")
    private String key;

    @Column(name = "request_hash")
    private String requestHash;

    @Column(name = "operation")
    private String operation;

    @Column(name = "consumed_at")
    private java.time.Instant consumedAt;

    @Column(name = "expires_at")
    private java.time.Instant expiresAt;

    @Column(name = "response_body")
    private String responseBody; // kept for backward compatibility (response snapshot body)

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_headers")
    private String responseHeaders; // JSON map of headers

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    private Long version;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public java.time.Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(java.time.Instant consumedAt) { this.consumedAt = consumedAt; }
    public java.time.Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
