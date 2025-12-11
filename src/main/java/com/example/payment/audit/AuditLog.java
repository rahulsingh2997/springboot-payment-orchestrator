package com.example.payment.audit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Version;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    private String action;
    private String resourceType;
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at")
    private java.time.Instant createdAt;

    @Column(name = "updated_at")
    private java.time.Instant updatedAt;

    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }
    public java.time.Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
