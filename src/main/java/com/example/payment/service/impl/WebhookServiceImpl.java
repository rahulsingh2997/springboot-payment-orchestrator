package com.example.payment.service.impl;

import com.example.payment.events.WebhookReceivedEvent;
import com.example.payment.persistence.WebhookEventEntity;
import com.example.payment.persistence.WebhookEventRepository;
import com.example.payment.persistence.enums.WebhookEventStatus;
import com.example.payment.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);

    private final WebhookEventRepository repository;
    private final ApplicationEventPublisher publisher;

    public WebhookServiceImpl(WebhookEventRepository repository, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public String handleWebhook(String payload, String source, String signatureHeader, String correlationId) {
        String id = UUID.randomUUID().toString();
        try {
            MDC.put("correlationId", correlationId);
            WebhookEventEntity ent = new WebhookEventEntity();
            ent.setId(id);
            ent.setSource(source);
            ent.setPayload(payload);
            ent.setStatus(WebhookEventStatus.RECEIVED);
            ent.setReceivedAt(Instant.now());
            repository.save(ent);

            // synchronous processing (placeholder) - mark processing and processed
            ent.setStatus(WebhookEventStatus.PROCESSING);
            repository.save(ent);

            // publish domain event for listeners
            publisher.publishEvent(new WebhookReceivedEvent(id, source, payload, correlationId, Instant.now(), "1.0"));

            ent.setStatus(WebhookEventStatus.PROCESSED);
            ent.setProcessedAt(Instant.now());
            repository.save(ent);

            log.info("webhook processed id={} source={} correlationId={}", id, source, correlationId);
            return id;
        } catch (Exception ex) {
            log.error("webhook processing failed correlationId={}", correlationId, ex);
            try {
                WebhookEventEntity ent = new WebhookEventEntity();
                ent.setId(id);
                ent.setSource(source);
                ent.setPayload(payload);
                ent.setStatus(WebhookEventStatus.FAILED);
                ent.setReceivedAt(Instant.now());
                ent.setProcessedAt(Instant.now());
                repository.save(ent);
            } catch (Exception e) {
                log.error("failed to persist failed webhook record", e);
            }
            throw ex;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
