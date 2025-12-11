package com.example.payment.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MetricsListener {

    private final MeterRegistry registry;

    public MetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @EventListener
    public void onPaymentCaptured(com.example.payment.events.PaymentCapturedEvent ev) {
        registry.counter("payment.captured.count").increment();
    }

    @EventListener
    public void onSubscriptionCharged(com.example.payment.events.SubscriptionChargedEvent ev) {
        registry.counter("subscription.charged.count").increment();
    }

    @EventListener
    public void onWebhookReceived(com.example.payment.events.WebhookReceivedEvent ev) {
        registry.counter("webhook.received.count").increment();
    }
}
