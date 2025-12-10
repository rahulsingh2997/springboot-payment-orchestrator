package com.example.payment.jobs;

import com.example.payment.persistence.SubscriptionEntity;
import com.example.payment.persistence.SubscriptionRepository;
import com.example.payment.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ReconciliationJob {

    private final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    public ReconciliationJob(SubscriptionService subscriptionService, SubscriptionRepository subscriptionRepository) {
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Scheduled(fixedDelayString = "${subscription.reconcile.interval.ms:60000}")
    public void run() {
        String cid = UUID.randomUUID().toString();
        MDC.put("correlationId", cid);
        log.info("ReconciliationJob starting correlationId={}", cid);

        int processed = 0;
        try {
            Instant now = Instant.now();
            List<SubscriptionEntity> due = subscriptionRepository.findByNextBillingAtBefore(now);
            for (SubscriptionEntity s : due) {
                try {
                    subscriptionService.renewSubscription(s.getId());
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to renew subscription {} correlationId={}", s.getId(), cid, e);
                }
            }
        } catch (Exception e) {
            log.error("Reconciliation job failed correlationId={}", cid, e);
        } finally {
            log.info("ReconciliationJob completed correlationId={} processed={}", cid, processed);
            MDC.remove("correlationId");
        }
    }
}
