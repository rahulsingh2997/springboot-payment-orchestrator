package com.example.payment.service.impl;

import com.example.payment.api.dto.OrderRequest;
import com.example.payment.api.dto.OrderResponse;
import com.example.payment.persistence.OrderEntity;
import com.example.payment.persistence.OrderRepository;
import com.example.payment.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired(required = false)
    private OrderRepository orderRepository;

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

