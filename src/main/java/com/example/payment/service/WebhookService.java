package com.example.payment.service;

public interface WebhookService {
    String handleWebhook(String payload, String source, String signatureHeader, String correlationId);
}
