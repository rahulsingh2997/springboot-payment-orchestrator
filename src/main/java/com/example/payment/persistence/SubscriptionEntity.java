package com.example.payment.persistence;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @Id
    private String id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "plan_id")
    private String planId;

    @Column(name = "amount_cents")
    private Long amountCents;

    @Column(name = "currency")
    private String currency;

    @Column(name = "status")
    private String status;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "next_billing_at")
    private Instant nextBillingAt;

    @Column(name = "last_renewed_at")
    private Instant lastRenewedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private Long version;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public Instant getNextBillingAt() { return nextBillingAt; }
    public void setNextBillingAt(Instant nextBillingAt) { this.nextBillingAt = nextBillingAt; }
    public Instant getLastRenewedAt() { return lastRenewedAt; }
    public void setLastRenewedAt(Instant lastRenewedAt) { this.lastRenewedAt = lastRenewedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
