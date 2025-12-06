package com.example.payment.persistence;

public enum OrderStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED,
    FAILED,
    CANCELLED
}
