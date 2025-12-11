package com.example.payment.service.impl;

import com.example.payment.persistence.SubscriptionEntity;
import com.example.payment.persistence.SubscriptionRepository;
import com.example.payment.persistence.TransactionEntity;
import com.example.payment.persistence.TransactionRepository;
import com.example.payment.persistence.enums.TransactionType;
import com.example.payment.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, TransactionRepository transactionRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public SubscriptionEntity createSubscription(SubscriptionEntity req) {
        String id = UUID.randomUUID().toString();
        req.setId(id);
        req.setStatus("ACTIVE");
        Instant now = Instant.now();
        req.setCreatedAt(now);
        req.setUpdatedAt(now);
        int interval = req.getIntervalDays() == null ? 30 : req.getIntervalDays();
        req.setIntervalDays(interval);
        req.setNextBillingAt(now.plusSeconds( (long)interval * 24 * 3600 ));
        subscriptionRepository.save(req);
        log.info("Created subscription {} for customer {} correlationId={}", id, req.getCustomerId(), MDC.get("correlationId"));

        if (meterRegistry != null) {
            try {
                meterRegistry.counter("subscription_created_total").increment();
            } catch (Exception ignore) {}
        }
        return req;
    }

    @Override
    public Optional<SubscriptionEntity> getSubscription(String id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    @Transactional
    public void renewDueSubscriptions() {
        Instant now = Instant.now();
        List<SubscriptionEntity> due = subscriptionRepository.findDueSubscriptions(now);
        for (SubscriptionEntity s : due) {
            try {
                renewSubscription(s.getId());
            } catch (Exception e) {
                log.error("Failed to renew subscription {}", s.getId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void renewSubscription(String id) {
        SubscriptionEntity s = subscriptionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        // create a capture transaction representing billing
        TransactionEntity t = new TransactionEntity();
        t.setId(UUID.randomUUID().toString());
        t.setOrderId(s.getId());
        t.setAmountCents(s.getAmountCents());
        t.setCurrency(s.getCurrency());
        t.setType(TransactionType.CAPTURE);
        t.setStatus("COMPLETED");
        t.setCreatedAt(Instant.now());
        transactionRepository.save(t);
        // advance nextBillingAt and update timestamps
        int interval = s.getIntervalDays() == null ? 30 : s.getIntervalDays();
        Instant now = Instant.now();
        s.setLastRenewedAt(now);
        s.setUpdatedAt(now);
        s.setNextBillingAt(s.getNextBillingAt() == null ? now.plusSeconds((long)interval * 24 * 3600) : s.getNextBillingAt().plusSeconds((long)interval * 24 * 3600));
        subscriptionRepository.save(s);

        // publish domain event for charged subscription
        if (eventPublisher != null) {
            String cid = MDC.get("correlationId");
            eventPublisher.publishEvent(new com.example.payment.events.SubscriptionChargedEvent(s.getId(), s.getAmountCents(), cid, Instant.now(), "1.0"));
        }

        log.info("Renewed subscription {} and created transaction {} correlationId={}", s.getId(), t.getId(), MDC.get("correlationId"));

        if (meterRegistry != null) {
            try {
                meterRegistry.counter("subscription_renewed_total").increment();
            } catch (Exception ignore) {}
        }
    }
}
