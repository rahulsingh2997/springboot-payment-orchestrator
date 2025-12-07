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
import java.util.UUID;

import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;
import com.example.payment.persistence.TransactionEntity;
import com.example.payment.persistence.TransactionRepository;
import com.example.payment.service.PaymentService;
import com.example.payment.events.PaymentCapturedEvent;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired(required = false)
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private TransactionRepository transactionRepository;

    @Autowired(required = false)
    private PaymentService paymentService;

    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

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
        if (paymentService != null) {
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

                        // publish in-memory event
                        if (eventPublisher != null) {
                            eventPublisher.publishEvent(new PaymentCapturedEvent(e.getId(), cap.getTransactionId(), e.getAmountCents()));
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

