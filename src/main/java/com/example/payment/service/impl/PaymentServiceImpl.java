package com.example.payment.service.impl;

import com.example.payment.gateway.authorize.AuthorizeNetGateway;
import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;
import com.example.payment.service.PaymentService;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final AuthorizeNetGateway authorizeNetGateway;

    public PaymentServiceImpl(AuthorizeNetGateway authorizeNetGateway) {
        this.authorizeNetGateway = authorizeNetGateway;
    }

    @Override
    public AuthorizeNetResponse authorize(AuthorizeNetRequest request) {
        if (authorizeNetGateway == null) return null;
        return authorizeNetGateway.authorize(request);
    }

    @Override
    public AuthorizeNetResponse capture(String transactionId) {
        if (authorizeNetGateway == null) return null;
        return authorizeNetGateway.capture(transactionId);
    }

    @Override
    public AuthorizeNetResponse refund(String transactionId, long amountCents) {
        if (authorizeNetGateway == null) return null;
        return authorizeNetGateway.refund(transactionId, amountCents);
    }

    @Override
    public AuthorizeNetResponse voidTransaction(String transactionId) {
        if (authorizeNetGateway == null) return null;
        return authorizeNetGateway.voidTransaction(transactionId);
    }
}
