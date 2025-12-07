package com.example.payment.service.impl;

import com.example.payment.gateway.authorize.AuthorizeNetGateway;
import com.example.payment.gateway.GatewayException;
import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;
import com.example.payment.service.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final AuthorizeNetGateway authorizeNetGateway;

    public PaymentServiceImpl(AuthorizeNetGateway authorizeNetGateway) {
        this.authorizeNetGateway = authorizeNetGateway;
    }

    @Override
    @Retry(name = "authorize-net")
    @CircuitBreaker(name = "authorize-net")
    public AuthorizeNetResponse authorize(AuthorizeNetRequest request) {
        if (authorizeNetGateway == null) return null;
        String cid = MDC.get("correlationId");
        log.info("[authorize] correlationId={} externalId={}", cid, request.getExternalId());
        try {
            return authorizeNetGateway.authorize(request);
        } catch (GatewayException ge) {
            log.error("GatewayException during authorize correlationId={} retryable={}", cid, ge.isRetryable(), ge);
            throw ge;
        } catch (Exception ex) {
            log.error("Unexpected exception during authorize correlationId={}", cid, ex);
            throw new GatewayException("Unexpected gateway error", ex, true);
        }
    }

    @Override
    @Retry(name = "authorize-net")
    @CircuitBreaker(name = "authorize-net")
    public AuthorizeNetResponse capture(String transactionId) {
        if (authorizeNetGateway == null) return null;
        String cid = MDC.get("correlationId");
        log.info("[capture] correlationId={} transactionId={}", cid, transactionId);
        try {
            return authorizeNetGateway.capture(transactionId);
        } catch (GatewayException ge) {
            log.error("GatewayException during capture correlationId={} retryable={}", cid, ge.isRetryable(), ge);
            throw ge;
        } catch (Exception ex) {
            log.error("Unexpected exception during capture correlationId={}", cid, ex);
            throw new GatewayException("Unexpected gateway error", ex, true);
        }
    }

    @Override
    @Retry(name = "authorize-net")
    @CircuitBreaker(name = "authorize-net")
    public AuthorizeNetResponse refund(String transactionId, long amountCents) {
        if (authorizeNetGateway == null) return null;
        String cid = MDC.get("correlationId");
        log.info("[refund] correlationId={} transactionId={} amountCents={}", cid, transactionId, amountCents);
        try {
            return authorizeNetGateway.refund(transactionId, amountCents);
        } catch (GatewayException ge) {
            log.error("GatewayException during refund correlationId={} retryable={}", cid, ge.isRetryable(), ge);
            throw ge;
        } catch (Exception ex) {
            log.error("Unexpected exception during refund correlationId={}", cid, ex);
            throw new GatewayException("Unexpected gateway error", ex, true);
        }
    }

    @Override
    @Retry(name = "authorize-net")
    @CircuitBreaker(name = "authorize-net")
    public AuthorizeNetResponse voidTransaction(String transactionId) {
        if (authorizeNetGateway == null) return null;
        String cid = MDC.get("correlationId");
        log.info("[void] correlationId={} transactionId={}", cid, transactionId);
        try {
            return authorizeNetGateway.voidTransaction(transactionId);
        } catch (GatewayException ge) {
            log.error("GatewayException during void correlationId={} retryable={}", cid, ge.isRetryable(), ge);
            throw ge;
        } catch (Exception ex) {
            log.error("Unexpected exception during void correlationId={}", cid, ex);
            throw new GatewayException("Unexpected gateway error", ex, true);
        }
    }
}
