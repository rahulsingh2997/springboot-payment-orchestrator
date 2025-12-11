package com.example.payment.service.impl;

import com.example.payment.api.dto.OrderRequest;
import com.example.payment.api.dto.OrderResponse;
import com.example.payment.persistence.OrderEntity;
import com.example.payment.persistence.OrderRepository;
import com.example.payment.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.example.payment.service.InvalidOrderStateException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import io.micrometer.core.instrument.MeterRegistry;

import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;
import com.example.payment.persistence.TransactionEntity;
import com.example.payment.persistence.TransactionRepository;
import com.example.payment.service.PaymentService;
import com.example.payment.persistence.enums.TransactionType;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private TransactionRepository transactionRepository;

    @Autowired(required = false)
    private PaymentService paymentService;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${payment.auto-purchase:false}")
    private boolean autoPurchase;

    @Override
    public OrderResponse createOrder(OrderRequest req) {
        OrderEntity e = new OrderEntity();
        e.setId(UUID.randomUUID().toString());
        e.setExternalOrderId(req.getExternalOrderId());
        e.setCustomerId(req.getCustomerId());
        e.setAmountCents(req.getAmountCents());
        e.setCurrency(req.getCurrency());
        e.setStatus("PENDING");
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        if (orderRepository != null) {
            orderRepository.save(e);
        }
        OrderResponse r = toResponse(e);

        // Perform purchase (authorize + capture) if PaymentService is available
        // and auto-purchase is enabled. By default auto-purchase is disabled
        // so newly created orders remain in PENDING state unless explicitly
        // enabled in configuration. This allows testing state transitions
        // (authorize -> capture) without an automatic capture on create.
        if (paymentService != null && autoPurchase) {
            try {
                AuthorizeNetRequest gReq = new AuthorizeNetRequest();
                // map amount cents to decimal string in dollars
                long cents = req.getAmountCents() == null ? 0L : req.getAmountCents();
                String amountDecimal = String.format("%.2f", cents / 100.0);
                gReq.setAmount(amountDecimal);
                gReq.setCurrency(req.getCurrency());
                gReq.setCustomerId(req.getCustomerId());
                gReq.setExternalId(req.getExternalOrderId());

                AuthorizeNetResponse auth = paymentService.authorize(gReq);
                if (auth != null && auth.isSuccess() && auth.getTransactionId() != null) {
                    AuthorizeNetResponse cap = paymentService.capture(auth.getTransactionId());
                    if (cap != null && cap.isSuccess()) {
                        // persist a transaction record if repo available
                        if (transactionRepository != null) {
                            TransactionEntity t = new TransactionEntity();
                            t.setId(UUID.randomUUID().toString());
                            t.setOrderId(e.getId());
                            t.setAmountCents(e.getAmountCents());
                            t.setCurrency(e.getCurrency());
                            t.setType(com.example.payment.persistence.enums.TransactionType.CAPTURE);
                            t.setStatus("CAPTURED");
                            t.setCreatedAt(Instant.now());
                            t.setUpdatedAt(Instant.now());
                            t.setGateway("authorize_net");
                            t.setGatewayTransactionId(cap.getTransactionId());
                            t.setGatewayResponse(cap.getMessage());
                            t.setGatewayMessage(cap.getMessage());
                            transactionRepository.save(t);
                        }

                        // update order status
                        e.setStatus("CAPTURED");
                        e.setUpdatedAt(Instant.now());
                        if (orderRepository != null) orderRepository.save(e);

                        // publish in-memory event (enriched with metadata)
                        if (eventPublisher != null) {
                            String cid = org.slf4j.MDC.get("correlationId");
                            eventPublisher.publishEvent(new com.example.payment.events.PaymentCapturedEvent(e.getId(), cap.getTransactionId(), e.getAmountCents(), cid, Instant.now(), "1.0"));
                        }
                        r = toResponse(e);
                    }
                }
            } catch (Exception ex) {
                // swallow and return created order; the baseline should not fail creation if gateway has issues
            }
        }

        return r;
    }

    @Override
    public OrderResponse getOrder(String id) {
        if (orderRepository == null) return null;
        return orderRepository.findById(id).map(this::toResponse).orElse(null);
    }

    @Override
    public OrderResponse authorizeOrder(String orderId) {
        if (orderRepository == null) return null;
        OrderEntity e = orderRepository.findById(orderId).orElse(null);
        if (e == null) return null;

        if (paymentService == null) return toResponse(e);

        // validate state: only PENDING -> AUTHORIZED
        if (!"PENDING".equalsIgnoreCase(e.getStatus())) {
            throw new InvalidOrderStateException("Order must be in PENDING state to authorize. Current=" + e.getStatus());
        }

        log.info("authorizeOrder invoked correlationId={} orderId={} status={}", MDC.get("correlationId"), orderId, e.getStatus());

        try {
            AuthorizeNetRequest gReq = new AuthorizeNetRequest();
            long cents = e.getAmountCents() == null ? 0L : e.getAmountCents();
            String amountDecimal = String.format("%.2f", cents / 100.0);
            gReq.setAmount(amountDecimal);
            gReq.setCurrency(e.getCurrency());
            gReq.setCustomerId(e.getCustomerId());
            gReq.setExternalId(e.getExternalOrderId());

            AuthorizeNetResponse auth = paymentService.authorize(gReq);
            if (auth != null && auth.isSuccess()) {
                if (transactionRepository != null) {
                    TransactionEntity t = new TransactionEntity();
                    t.setId(UUID.randomUUID().toString());
                    t.setOrderId(e.getId());
                    t.setAmountCents(e.getAmountCents());
                    t.setCurrency(e.getCurrency());
                    t.setType(TransactionType.AUTHORIZATION);
                    t.setStatus("AUTHORIZED");
                    t.setCreatedAt(Instant.now());
                    t.setUpdatedAt(Instant.now());
                    t.setGateway("authorize_net");
                    t.setGatewayTransactionId(auth.getTransactionId());
                    String resp = auth.getMessage();
                    if (resp != null && resp.length() > 1024) resp = resp.substring(0, 1024);
                    t.setGatewayResponse(resp);
                    t.setGatewayMessage(resp);
                    transactionRepository.save(t);
                }

                e.setStatus("AUTHORIZED");
                e.setUpdatedAt(Instant.now());
                if (orderRepository != null) orderRepository.save(e);

                if (meterRegistry != null) {
                    try {
                        meterRegistry.counter("payment_authorized_total").increment();
                    } catch (Exception ignore) {}
                }

                if (eventPublisher != null) {
                    // enrich event with correlationId, timestamp and schema version
                    String cid = org.slf4j.MDC.get("correlationId");
                    eventPublisher.publishEvent(new com.example.payment.events.PaymentAuthorizedEvent(e.getId(), auth.getTransactionId(), e.getAmountCents(), cid, Instant.now(), "1.0"));
                }
            }
        } catch (Exception ex) {
            // swallow to preserve behavior
        }

        return toResponse(e);
    }

    @Override
    public OrderResponse captureOrder(String orderId) {
        if (orderRepository == null || transactionRepository == null) return null;
        OrderEntity e = orderRepository.findById(orderId).orElse(null);
        if (e == null) return null;
        if (paymentService == null) return toResponse(e);

        // validate state: only AUTHORIZED -> CAPTURED
        if (!"AUTHORIZED".equalsIgnoreCase(e.getStatus())) {
            throw new InvalidOrderStateException("Order must be in AUTHORIZED state to capture. Current=" + e.getStatus());
        }

        log.info("captureOrder invoked correlationId={} orderId={} status={}", MDC.get("correlationId"), orderId, e.getStatus());

        try {
            java.util.List<TransactionEntity> auths = transactionRepository.findByOrderIdAndTypeOrderByCreatedAtDesc(orderId, TransactionType.AUTHORIZATION);
            if (auths == null || auths.isEmpty()) return toResponse(e);
            TransactionEntity auth = auths.get(0);
            if (auth.getGatewayTransactionId() == null) return toResponse(e);

            AuthorizeNetResponse cap = paymentService.capture(auth.getGatewayTransactionId());
            if (cap != null && cap.isSuccess()) {
                if (transactionRepository != null) {
                    TransactionEntity t = new TransactionEntity();
                    t.setId(UUID.randomUUID().toString());
                    t.setOrderId(e.getId());
                    t.setAmountCents(e.getAmountCents());
                    t.setCurrency(e.getCurrency());
                    t.setType(TransactionType.CAPTURE);
                    t.setStatus("CAPTURED");
                    t.setCreatedAt(Instant.now());
                    t.setUpdatedAt(Instant.now());
                    t.setGateway("authorize_net");
                    t.setGatewayTransactionId(cap.getTransactionId());
                    String resp = cap.getMessage();
                    if (resp != null && resp.length() > 1024) resp = resp.substring(0, 1024);
                    t.setGatewayResponse(resp);
                    t.setGatewayMessage(resp);
                    transactionRepository.save(t);
                }

                e.setStatus("CAPTURED");
                e.setUpdatedAt(Instant.now());
                if (orderRepository != null) orderRepository.save(e);

                        if (meterRegistry != null) {
                            try {
                                meterRegistry.counter("payment_captured_total").increment();
                            } catch (Exception ignore) {}
                        }

                        if (eventPublisher != null) {
                            String cid = org.slf4j.MDC.get("correlationId");
                            eventPublisher.publishEvent(new com.example.payment.events.PaymentCapturedEvent(e.getId(), cap.getTransactionId(), e.getAmountCents(), cid, Instant.now(), "1.0"));
                        }
            }
        } catch (Exception ex) {
            // swallow
        }

        return toResponse(e);
    }

    @Override
    public OrderResponse voidOrder(String orderId) {
        if (orderRepository == null || transactionRepository == null) return null;
        OrderEntity e = orderRepository.findById(orderId).orElse(null);
        if (e == null) return null;
        if (paymentService == null) return toResponse(e);

        // validate state: only AUTHORIZED -> VOIDED
        if (!"AUTHORIZED".equalsIgnoreCase(e.getStatus())) {
            throw new InvalidOrderStateException("Order must be in AUTHORIZED state to void. Current=" + e.getStatus());
        }

        log.info("voidOrder invoked correlationId={} orderId={} status={}", MDC.get("correlationId"), orderId, e.getStatus());

        try {
            java.util.List<TransactionEntity> auths = transactionRepository.findByOrderIdAndTypeOrderByCreatedAtDesc(orderId, TransactionType.AUTHORIZATION);
            if (auths == null || auths.isEmpty()) return toResponse(e);
            TransactionEntity auth = auths.get(0);
            if (auth.getGatewayTransactionId() == null) return toResponse(e);

            AuthorizeNetResponse v = paymentService.voidTransaction(auth.getGatewayTransactionId());
            if (v != null && v.isSuccess()) {
                if (transactionRepository != null) {
                    TransactionEntity t = new TransactionEntity();
                    t.setId(UUID.randomUUID().toString());
                    t.setOrderId(e.getId());
                    t.setAmountCents(0L);
                    t.setCurrency(e.getCurrency());
                    t.setType(TransactionType.VOID);
                    t.setStatus("VOIDED");
                    t.setCreatedAt(Instant.now());
                    t.setUpdatedAt(Instant.now());
                    t.setGateway("authorize_net");
                    t.setGatewayTransactionId(v.getTransactionId());
                    String resp = v.getMessage();
                    if (resp != null && resp.length() > 1024) resp = resp.substring(0, 1024);
                    t.setGatewayResponse(resp);
                    t.setGatewayMessage(resp);
                    transactionRepository.save(t);
                }

                e.setStatus("VOIDED");
                e.setUpdatedAt(Instant.now());
                if (orderRepository != null) orderRepository.save(e);

                if (eventPublisher != null) {
                    String cid = org.slf4j.MDC.get("correlationId");
                    eventPublisher.publishEvent(new com.example.payment.events.PaymentVoidedEvent(e.getId(), v.getTransactionId(), cid, Instant.now(), "1.0"));
                }

                    if (meterRegistry != null) {
                        try {
                            meterRegistry.counter("payment_voided_total").increment();
                        } catch (Exception ignore) {}
                    }
            }
        } catch (Exception ex) {
            // swallow
        }

        return toResponse(e);
    }

    @Override
    public OrderResponse refundOrder(String orderId, Long amountCents) {
        if (orderRepository == null || transactionRepository == null) return null;
        OrderEntity e = orderRepository.findById(orderId).orElse(null);
        if (e == null) return null;
        if (paymentService == null) return toResponse(e);

        // validate state: only CAPTURED -> REFUNDED
        if (!"CAPTURED".equalsIgnoreCase(e.getStatus())) {
            throw new InvalidOrderStateException("Order must be in CAPTURED state to refund. Current=" + e.getStatus());
        }

        log.info("refundOrder invoked correlationId={} orderId={} status={}", MDC.get("correlationId"), orderId, e.getStatus());

        try {
            java.util.List<TransactionEntity> caps = transactionRepository.findByOrderIdAndTypeOrderByCreatedAtDesc(orderId, TransactionType.CAPTURE);
            if (caps == null || caps.isEmpty()) return toResponse(e);
            TransactionEntity capTx = caps.get(0);
            if (capTx.getGatewayTransactionId() == null) return toResponse(e);

            long refundAmount = (amountCents == null) ? (e.getAmountCents() == null ? 0L : e.getAmountCents()) : amountCents;
            AuthorizeNetResponse ref = paymentService.refund(capTx.getGatewayTransactionId(), refundAmount);
            if (ref != null && ref.isSuccess()) {
                if (transactionRepository != null) {
                    TransactionEntity t = new TransactionEntity();
                    t.setId(UUID.randomUUID().toString());
                    t.setOrderId(e.getId());
                    t.setAmountCents(refundAmount);
                    t.setCurrency(e.getCurrency());
                    t.setType(TransactionType.REFUND);
                    t.setStatus("REFUNDED");
                    t.setCreatedAt(Instant.now());
                    t.setUpdatedAt(Instant.now());
                    t.setGateway("authorize_net");
                    t.setGatewayTransactionId(ref.getTransactionId());
                    String resp = ref.getMessage();
                    if (resp != null && resp.length() > 1024) resp = resp.substring(0, 1024);
                    t.setGatewayResponse(resp);
                    t.setGatewayMessage(resp);
                    transactionRepository.save(t);
                }

                e.setStatus("REFUNDED");
                e.setUpdatedAt(Instant.now());
                if (orderRepository != null) orderRepository.save(e);

                if (eventPublisher != null) {
                    String cid = org.slf4j.MDC.get("correlationId");
                    eventPublisher.publishEvent(new com.example.payment.events.PaymentRefundedEvent(e.getId(), ref.getTransactionId(), refundAmount, cid, Instant.now(), "1.0"));
                }

                    if (meterRegistry != null) {
                        try {
                            meterRegistry.counter("payment_refunded_total").increment();
                        } catch (Exception ignore) {}
                    }
            }
        } catch (Exception ex) {
            // swallow
        }

        return toResponse(e);
    }

    private OrderResponse toResponse(OrderEntity e) {
        OrderResponse r = new OrderResponse();
        r.setId(e.getId());
        r.setExternalOrderId(e.getExternalOrderId());
        r.setCustomerId(e.getCustomerId());
        r.setAmountCents(e.getAmountCents());
        r.setCurrency(e.getCurrency());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}

