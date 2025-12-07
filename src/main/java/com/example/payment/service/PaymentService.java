package com.example.payment.service;

import com.example.payment.gateway.mapper.AuthorizeNetRequest;
import com.example.payment.gateway.mapper.AuthorizeNetResponse;

public interface PaymentService {
    AuthorizeNetResponse authorize(AuthorizeNetRequest request);
    AuthorizeNetResponse capture(String transactionId);
    AuthorizeNetResponse refund(String transactionId, long amountCents);
    AuthorizeNetResponse voidTransaction(String transactionId);
}
