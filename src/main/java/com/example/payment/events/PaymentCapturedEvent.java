package com.example.payment.events;

public class PaymentCapturedEvent {
    private final String orderId;
    private final String transactionId;
    private final Long amountCents;

    public PaymentCapturedEvent(String orderId, String transactionId, Long amountCents) {
        this.orderId = orderId;
        this.transactionId = transactionId;
        this.amountCents = amountCents;
    }

    public String getOrderId() { return orderId; }
    public String getTransactionId() { return transactionId; }
    public Long getAmountCents() { return amountCents; }
}
