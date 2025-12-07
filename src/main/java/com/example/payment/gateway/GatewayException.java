package com.example.payment.gateway;

public class GatewayException extends RuntimeException {
    private final boolean retryable;

    public GatewayException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public GatewayException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
