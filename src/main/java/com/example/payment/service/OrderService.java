package com.example.payment.service;

import com.example.payment.api.dto.OrderRequest;
import com.example.payment.api.dto.OrderResponse;

public interface OrderService {
    OrderResponse createOrder(OrderRequest req);
    OrderResponse getOrder(String id);
}
